package com.dbook.domain.booking

/** What the bookings of one [com.dbook.domain.catalog.Bookable] mean for changing or cancelling it. */
interface BookingOccupancy {
    /** The seats that ever had a booking, of any status: they are referenced by it and cannot be deleted. */
    fun bookedSeatIds(bookableId: Long): Set<Long>

    /** The bookings still alive (PENDING or CONFIRMED) of the bookable and their owners: who to tell of a change. */
    fun activeBookingOwners(bookableId: Long): List<ActiveBooking>

    /** Bookings still alive: PENDING (holding a seat) or CONFIRMED (paid). */
    fun activeBookings(bookableId: Long): Long
}

data class ActiveBooking(
    val bookingId: Long,
    val customerId: Long,
)
