package com.dbook.application.payment

import com.dbook.application.booking.BookingInventoryReleaser
import com.dbook.application.common.afterCommit
import com.dbook.application.common.countOutcome
import com.dbook.domain.booking.BookingNotFoundException
import com.dbook.domain.booking.BookingRepository
import com.dbook.domain.common.access.Actor
import com.dbook.domain.common.audit.AuditAction
import com.dbook.domain.common.audit.AuditEvent
import com.dbook.domain.common.audit.AuditLog
import com.dbook.domain.messaging.OutboxWriter
import com.dbook.domain.payment.Refund
import com.dbook.domain.payment.RefundEvents
import com.dbook.domain.payment.RefundNotFoundException
import com.dbook.domain.payment.RefundRepository
import com.dbook.domain.payment.RefundStatus
import com.dbook.domain.payment.toAuditSnapshot
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

// The last step of the refund saga, after the gateway answered. Completing is one transaction: the refund, the booking
// going REFUNDED and the seat coming back all happen together or not at all. Both steps are safe to repeat (a retry
// after a crash finds the work already done and returns it).
@Service
class RefundSettler(
    private val refundRepository: RefundRepository,
    private val bookingRepository: BookingRepository,
    private val inventory: BookingInventoryReleaser,
    private val auditLog: AuditLog,
    private val outboxWriter: OutboxWriter,
    private val meterRegistry: MeterRegistry,
    private val clock: Clock,
) {
    @Transactional
    fun complete(
        refundId: Long,
        actor: Actor,
    ): Refund {
        val refund = refundRepository.findById(refundId) ?: throw RefundNotFoundException(refundId)
        if (refund.status == RefundStatus.COMPLETED) {
            return refund
        }
        val booking =
            bookingRepository.findById(refund.bookingId) ?: throw BookingNotFoundException(refund.bookingId)
        val completed = refundRepository.save(refund.complete(clock.instant()))
        bookingRepository.save(booking.refund())
        inventory.release(booking)
        record(actor, AuditAction.REFUND_COMPLETED, refund, completed)
        // the customer is told in the same transaction that gave the money back
        outboxWriter.add(RefundEvents.completed(completed, booking.customerId, booking.bookable.title, clock.instant()))

        inventory.announceAfterCommit(booking)
        afterCommit { meterRegistry.countOutcome("dbook.refund", "completed") }
        return completed
    }

    @Transactional
    fun fail(
        refundId: Long,
        actor: Actor,
        why: String,
    ): Refund {
        val refund = refundRepository.findById(refundId) ?: throw RefundNotFoundException(refundId)
        if (refund.status != RefundStatus.REQUESTED) {
            return refund
        }
        val failed = refundRepository.save(refund.fail(why))
        record(actor, AuditAction.REFUND_FAILED, refund, failed)
        afterCommit { meterRegistry.countOutcome("dbook.refund", "failed") }
        return failed
    }

    private fun record(
        actor: Actor,
        action: AuditAction,
        before: Refund,
        after: Refund,
    ) = auditLog.record(
        AuditEvent(
            actor = actor,
            action = action,
            targetId = requireNotNull(after.id).toString(),
            before = before.toAuditSnapshot(),
            after = after.toAuditSnapshot(),
        ),
    )
}
