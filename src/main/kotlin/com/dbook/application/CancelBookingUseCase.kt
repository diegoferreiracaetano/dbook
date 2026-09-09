package com.dbook.application

import com.dbook.domain.AvailabilityBroadcaster
import com.dbook.domain.BookableRepository
import com.dbook.domain.Booking
import com.dbook.domain.BookingNotFoundException
import com.dbook.domain.BookingRepository
import com.dbook.domain.NotBookingOwnerException
import com.dbook.domain.Role
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CancelBookingUseCase(
    private val bookingRepository: BookingRepository,
    private val bookableRepository: BookableRepository,
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
        val updatedBookable = bookableRepository.incrementAvailability(bookableId)
        val saved = bookingRepository.save(cancelled)

        afterCommit { availabilityBroadcaster.broadcast(bookableId, updatedBookable.availableCapacity) }
        return saved
    }
}
