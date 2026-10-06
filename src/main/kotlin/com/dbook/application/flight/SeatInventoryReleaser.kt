package com.dbook.application.flight

import com.dbook.application.common.afterCommit
import com.dbook.domain.booking.AvailabilityBroadcaster
import com.dbook.domain.booking.Booking
import com.dbook.domain.booking.InventoryReleaser
import com.dbook.domain.seating.SeatRepository
import org.springframework.stereotype.Service

/** A flight booking holds a seat: it is given back, and the new number of free seats is announced after the commit. */
@Service
class SeatInventoryReleaser(
    private val seatRepository: SeatRepository,
    private val availabilityBroadcaster: AvailabilityBroadcaster,
) : InventoryReleaser {
    override fun supports(booking: Booking): Boolean = booking.seatId != null

    override fun release(booking: Booking) {
        seatRepository.release(requireNotNull(booking.seatId))
    }

    override fun announceAfterCommit(booking: Booking) {
        val bookableId =
            requireNotNull(booking.bookable.id) { "A persisted Booking must reference a persisted Bookable" }
        afterCommit { availabilityBroadcaster.broadcast(bookableId, seatRepository.countAvailable(bookableId)) }
    }
}
