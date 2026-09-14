package com.dbook.domain

/** Persistence port for [Booking]. */
interface BookingRepository {
    fun findById(id: Long): Booking?

    /** Every booking made by [customerId] — used by `GET /bookings` ("my trips"). */
    fun findByCustomerId(customerId: Long): List<Booking>

    fun save(booking: Booking): Booking
}
