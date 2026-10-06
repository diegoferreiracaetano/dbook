package com.dbook.application.accommodation

import com.dbook.domain.accommodation.RoomInventory
import com.dbook.domain.booking.Booking
import com.dbook.domain.booking.InventoryReleaser
import org.springframework.stereotype.Service

/** A stay holds the nights of a room type: they are given back. There is no live number to announce for a stay. */
@Service
class StayInventoryReleaser(
    private val roomInventory: RoomInventory,
) : InventoryReleaser {
    override fun supports(booking: Booking): Boolean = booking.stay != null

    override fun release(booking: Booking) {
        roomInventory.release(requireNotNull(booking.stay))
    }
}
