package com.dbook.application.payment

import com.dbook.domain.booking.Booking
import com.dbook.domain.booking.BookingNotFoundException
import com.dbook.domain.booking.BookingRepository
import com.dbook.domain.booking.BookingStatus
import com.dbook.domain.booking.NotBookingOwnerException
import com.dbook.domain.catalog.Flight
import com.dbook.domain.payment.RefundPolicy
import com.dbook.domain.payment.RefundRepository
import io.micrometer.observation.annotation.Observed
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

/** Tells the owner of a booking what they can do about it, how much they get back and until when. */
@Observed(name = "dbook.usecase")
@Service
class GetCancellationPolicyUseCase(
    private val bookingRepository: BookingRepository,
    private val refundRepository: RefundRepository,
    private val clock: Clock,
) {
    @Transactional(readOnly = true)
    fun execute(
        userId: Long,
        bookingId: Long,
    ): CancellationPolicy {
        val booking = bookingRepository.findById(bookingId) ?: throw BookingNotFoundException(bookingId)
        if (booking.customerId != userId) {
            throw NotBookingOwnerException(bookingId)
        }
        return when (booking.status) {
            BookingStatus.PENDING -> CancellationPolicy(bookingId, CancellationAction.CANCEL, null, null, null)
            BookingStatus.CANCELLED -> blocked(booking, CancellationBlocker.ALREADY_CANCELLED)
            BookingStatus.REFUNDED -> blocked(booking, CancellationBlocker.ALREADY_REFUNDED)
            BookingStatus.CONFIRMED -> confirmedPolicy(booking)
        }
    }

    private fun confirmedPolicy(booking: Booking): CancellationPolicy {
        val bookingId = requireNotNull(booking.id)
        val departure = (booking.bookable as? Flight)?.departureTime
        val until = departure?.minus(RefundPolicy.FULL_REFUND_WINDOW)
        return when {
            refundRepository.findInProgressByBookingId(bookingId) != null ->
                blocked(booking, CancellationBlocker.REFUND_IN_PROGRESS, until)
            RefundPolicy.needsOverride(departure, LocalDateTime.now(clock)) ->
                blocked(booking, CancellationBlocker.WINDOW_CLOSED, until)
            else -> CancellationPolicy(bookingId, CancellationAction.REFUND_REQUEST, booking.paidAmount, until, null)
        }
    }

    private fun blocked(
        booking: Booking,
        reason: CancellationBlocker,
        until: LocalDateTime? = null,
    ) = CancellationPolicy(
        bookingId = requireNotNull(booking.id),
        action = CancellationAction.NONE,
        refundAmount = booking.paidAmount.takeIf { booking.status != BookingStatus.CANCELLED },
        refundableUntil = until,
        blockedBy = reason,
    )
}
