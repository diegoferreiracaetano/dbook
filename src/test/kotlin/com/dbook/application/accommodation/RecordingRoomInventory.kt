package com.dbook.application.accommodation

import com.dbook.domain.accommodation.RoomInventory
import com.dbook.domain.accommodation.RoomUnavailableException
import com.dbook.domain.booking.Stay
import java.time.LocalDate

/** Counts rooms per night in memory, with the quantity of every room type given up front. */
class RecordingRoomInventory(private val quantities: Map<Long, Int> = emptyMap()) : RoomInventory {
    val booked = mutableMapOf<Pair<Long, LocalDate>, Int>()
    val released = mutableListOf<Stay>()

    override fun reserve(stay: Stay) {
        val taken = mutableListOf<LocalDate>()
        try {
            stay.nightDates().forEach { night ->
                val key = stay.roomTypeId to night
                if ((booked[key] ?: 0) >= (quantities[stay.roomTypeId] ?: 1)) throw RoomUnavailableException(night)
                booked[key] = (booked[key] ?: 0) + 1
                taken += night
            }
        } catch (ex: RoomUnavailableException) {
            // what the transaction rollback does for the real inventory
            taken.forEach { booked[stay.roomTypeId to it] = (booked[stay.roomTypeId to it] ?: 1) - 1 }
            throw ex
        }
    }

    override fun release(stay: Stay) {
        released += stay
        stay.nightDates().forEach {
            val key = stay.roomTypeId to it
            booked[key] = maxOf(0, (booked[key] ?: 0) - 1)
        }
    }

    override fun isAvailable(stay: Stay): Boolean =
        stay.nightDates().all { (booked[stay.roomTypeId to it] ?: 0) < (quantities[stay.roomTypeId] ?: 1) }

    override fun maxBookedFrom(
        roomTypeId: Long,
        from: LocalDate,
    ): Int = booked.filterKeys { it.first == roomTypeId && !it.second.isBefore(from) }.values.maxOrNull() ?: 0
}
