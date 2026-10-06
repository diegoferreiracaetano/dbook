package com.dbook.domain.accommodation

import com.dbook.domain.booking.Stay
import java.time.LocalDate

class RoomUnavailableException(night: LocalDate) :
    RuntimeException("No room of this type is left for the night of $night")

/**
 * How many rooms of each type are taken on each night. Taking a stay raises the count of every one of its nights,
 * **each with one conditional statement** (it raises only while there is a room left), in a transaction: if any
 * night is full the whole stay is refused and the nights already taken are undone with it. Nights are always taken
 * in order, so two stays that overlap never wait for each other in a circle.
 */
interface RoomInventory {
    /** @throws RoomUnavailableException with the first full night; it joins the caller's transaction (needs one). */
    fun reserve(stay: Stay)

    /** Gives the nights of the stay back. Safe on a stay already released: no count goes below zero. */
    fun release(stay: Stay)

    /** Whether every night of the stay has a room left, without taking any (for the search and the preview). */
    fun isAvailable(stay: Stay): Boolean

    /** The most rooms of the type taken on any night from [from] on (what the quantity cannot go below). */
    fun maxBookedFrom(
        roomTypeId: Long,
        from: LocalDate,
    ): Int
}
