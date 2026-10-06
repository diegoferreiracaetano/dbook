package com.dbook.application.booking

import com.dbook.domain.booking.Booking
import com.dbook.domain.booking.BookingEvents
import com.dbook.domain.booking.BookingNotFoundException
import com.dbook.domain.booking.BookingRepository
import com.dbook.domain.booking.NotBookingOwnerException
import com.dbook.domain.booking.toAuditSnapshot
import com.dbook.domain.common.access.Actor
import com.dbook.domain.common.access.Permission.BOOKING_CANCEL_ANY
import com.dbook.domain.common.access.Role
import com.dbook.domain.common.audit.AuditAction
import com.dbook.domain.common.audit.AuditEvent
import com.dbook.domain.common.audit.AuditLog
import com.dbook.domain.messaging.OutboxWriter
import io.micrometer.observation.annotation.Observed
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

/** Why a booking is being cancelled: someone asked for it, or its time ran out. */
enum class CancellationSource { REQUESTED, EXPIRATION }

/** Cancels a PENDING [Booking] and releases its [com.dbook.domain.seating.Seat] back to AVAILABLE. */
@Observed(name = "dbook.usecase")
@Service
class CancelBookingUseCase(
    private val bookingRepository: BookingRepository,
    private val inventory: BookingInventoryReleaser,
    private val auditLog: AuditLog,
    private val outboxWriter: OutboxWriter,
    private val clock: Clock,
) {
    // A CLIENT may only cancel their own booking; whoever holds BOOKING_CANCEL_ANY can cancel any booking.
    // Without this check, authentication alone wouldn't actually protect a booking
    // from being cancelled by an unrelated authenticated user.
    @Transactional
    fun execute(
        bookingId: Long,
        requestingUserId: Long,
        requestingUserRole: Role,
        source: CancellationSource = CancellationSource.REQUESTED,
    ): Booking {
        val booking =
            bookingRepository.findById(bookingId)
                ?: throw BookingNotFoundException(bookingId)
        if (!requestingUserRole.can(BOOKING_CANCEL_ANY) && booking.customerId != requestingUserId) {
            throw NotBookingOwnerException(bookingId)
        }
        val cancelled = booking.cancel()
        inventory.release(booking)
        val saved = bookingRepository.save(cancelled)
        if (booking.customerId != requestingUserId) {
            recordStaffCancellation(booking, saved, Actor(requestingUserId, requestingUserRole))
        }
        announce(source, booking, saved, requestingUserId)

        inventory.announceAfterCommit(booking)
        return saved
    }

    // The customer cancelling their own booking is told nothing (they just did it); staff cancelling it, and the
    // expiration, are news to the customer. The event commits with the cancellation.
    private fun announce(
        source: CancellationSource,
        before: Booking,
        saved: Booking,
        requestingUserId: Long,
    ) {
        val event =
            when {
                source == CancellationSource.EXPIRATION -> BookingEvents.expired(saved, clock.instant())
                before.customerId != requestingUserId -> BookingEvents.cancelledByStaff(saved, clock.instant())
                else -> null
            }
        event?.let { outboxWriter.add(it) }
    }

    // reaching here with someone else's booking means the caller holds BOOKING_CANCEL_ANY: that is staff action
    private fun recordStaffCancellation(
        before: Booking,
        after: Booking,
        actor: Actor,
    ) = auditLog.record(
        AuditEvent(
            actor = actor,
            action = AuditAction.BOOKING_CANCELLED_BY_STAFF,
            targetId = requireNotNull(before.id).toString(),
            before = before.toAuditSnapshot(),
            after = after.toAuditSnapshot(),
        ),
    )
}
