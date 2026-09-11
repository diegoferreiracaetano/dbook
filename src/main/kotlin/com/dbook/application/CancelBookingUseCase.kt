package com.dbook.application

import com.dbook.domain.AvailabilityBroadcaster
import com.dbook.domain.Booking
import com.dbook.domain.BookingNotFoundException
import com.dbook.domain.BookingRepository
import com.dbook.domain.NotBookingOwnerException
import com.dbook.domain.Role
import com.dbook.domain.SeatRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** Cancels a PENDING [Booking] and releases its [com.dbook.domain.Seat] back to AVAILABLE. */
@Service
class CancelBookingUseCase(
    private val bookingRepository: BookingRepository,
    private val seatRepository: SeatRepository,
    private val availabilityBroadcaster: AvailabilityBroadcaster,
) {
    // A CLIENT may only cancel their own booking; ADMIN can cancel any booking.
    // Without this check, authentication alone wouldn't actually protect a booking
    // from being cancelled by an unrelated authenticated user.
    @Transactional
    fun execute(
        bookingId: Long,
        requestingUserId: Long,
        requestingUserRole: Role,
    ): Booking {
        val booking =
            bookingRepository.findById(bookingId)
                ?: throw BookingNotFoundException(bookingId)
        if (requestingUserRole != Role.ADMIN && booking.customerId != requestingUserId) {
            throw NotBookingOwnerException(bookingId)
        }
        val cancelled = booking.cancel()
        val bookableId =
            requireNotNull(booking.bookable.id) { "A persisted Booking must reference a persisted Bookable" }
        seatRepository.release(booking.seatId)
        val saved = bookingRepository.save(cancelled)

        afterCommit {
            availabilityBroadcaster.broadcast(bookableId, seatRepository.countAvailable(bookableId))
        }
        return saved
    }
}
