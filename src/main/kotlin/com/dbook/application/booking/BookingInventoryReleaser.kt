package com.dbook.application.booking

import com.dbook.application.common.afterCommit
import com.dbook.domain.accommodation.RoomInventory
import com.dbook.domain.booking.AvailabilityBroadcaster
import com.dbook.domain.booking.Booking
import com.dbook.domain.seating.SeatRepository
import org.springframework.stereotype.Service

/**
 * Gives back what a booking was holding when it ends (cancelled, expired or refunded): the seat of a flight, or the
 * nights of a stay. Inside the transaction of whatever ended the booking. For a flight the new number of free seats is
 * announced after the commit; a stay has no live number to announce (its availability is a question of the night).
 */
@Service
class BookingInventoryReleaser(
    private val seatRepository: SeatRepository,
    private val roomInventory: RoomInventory,
    private val availabilityBroadcaster: AvailabilityBroadcaster,
) {
    fun release(booking: Booking) {
        booking.seatId?.let { seatRepository.release(it) }
        booking.stay?.let { roomInventory.release(it) }
    }

    /** Call after [release], in the same transaction: tells the subscribers of a flight how many seats are free now. */
    fun announceAfterCommit(booking: Booking) {
        if (booking.seatId == null) {
            return
        }
        val bookableId =
            requireNotNull(booking.bookable.id) { "A persisted Booking must reference a persisted Bookable" }
        afterCommit { availabilityBroadcaster.broadcast(bookableId, seatRepository.countAvailable(bookableId)) }
    }
}
