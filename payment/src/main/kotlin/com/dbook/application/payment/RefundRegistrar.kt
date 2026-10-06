package com.dbook.application.payment

import com.dbook.domain.booking.Booking
import com.dbook.domain.booking.BookingNotFoundException
import com.dbook.domain.booking.BookingRepository
import com.dbook.domain.booking.BookingStatus
import com.dbook.domain.common.access.Actor
import com.dbook.domain.common.access.Role
import com.dbook.domain.common.audit.AuditAction
import com.dbook.domain.common.audit.AuditEvent
import com.dbook.domain.common.audit.AuditLog
import com.dbook.domain.flight.Flight
import com.dbook.domain.payment.Refund
import com.dbook.domain.payment.RefundNotFoundException
import com.dbook.domain.payment.RefundOverrideNotAllowedException
import com.dbook.domain.payment.RefundPolicy
import com.dbook.domain.payment.RefundRepository
import com.dbook.domain.payment.RefundStatus
import com.dbook.domain.payment.RefundWindowClosedException
import com.dbook.domain.payment.toAuditSnapshot
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

// The first step of the refund saga: the refund exists, REQUESTED and committed, before anyone talks to the gateway.
// If the process dies after this, the refund is there to be retried; if it did not exist yet, a crash would leave
// money moved with no record of why.
@Service
class RefundRegistrar(
    private val bookingRepository: BookingRepository,
    private val refundRepository: RefundRepository,
    private val auditLog: AuditLog,
    private val clock: Clock,
) {
    @Transactional
    fun register(command: RefundCommand): Refund {
        val booking =
            bookingRepository.findById(command.bookingId) ?: throw BookingNotFoundException(command.bookingId)
        check(booking.status == BookingStatus.CONFIRMED) { "Only a CONFIRMED booking can be refunded" }
        val paymentId = checkNotNull(booking.paymentId) { "A CONFIRMED booking has a payment" }
        enforceWindow(command, booking)

        val refund =
            refundRepository.save(
                Refund(
                    paymentId = paymentId,
                    bookingId = command.bookingId,
                    amount = booking.paidAmount,
                    reason = command.reason,
                    note = command.note?.trim()?.takeIf { it.isNotEmpty() },
                    idempotencyKey = command.idempotencyKey,
                    requestFingerprint = command.fingerprint(),
                    requestedBy = command.actor.id,
                    createdAt = clock.instant(),
                ),
            )
        record(command.actor, AuditAction.REFUND_REQUESTED, refund, before = null)
        return refund
    }

    /** A FAILED refund goes back to REQUESTED to be tried again; one still REQUESTED (a crash) is simply retried. */
    @Transactional
    fun reopen(
        refundId: Long,
        actor: Actor,
    ): Refund {
        val refund = refundRepository.findById(refundId) ?: throw RefundNotFoundException(refundId)
        if (refund.status == RefundStatus.REQUESTED) {
            return refund
        }
        val reopened = refundRepository.save(refund.reopen())
        record(actor, AuditAction.REFUND_RETRIED, reopened, before = refund)
        return reopened
    }

    private fun enforceWindow(
        command: RefundCommand,
        booking: Booking,
    ) {
        val departure = (booking.bookable as? Flight)?.departureTime
        if (!RefundPolicy.needsOverride(departure, LocalDateTime.now(clock))) {
            return
        }
        if (!command.override) {
            throw RefundWindowClosedException()
        }
        if (command.actor.role != Role.SUPER_ADMIN) {
            throw RefundOverrideNotAllowedException()
        }
        require(!command.note.isNullOrBlank()) { "an override needs a note saying why" }
    }

    private fun record(
        actor: Actor,
        action: AuditAction,
        refund: Refund,
        before: Refund?,
    ) = auditLog.record(
        AuditEvent(
            actor = actor,
            action = action,
            targetId = requireNotNull(refund.id).toString(),
            before = before?.toAuditSnapshot(),
            after = refund.toAuditSnapshot(),
            reason = refund.note,
        ),
    )
}
