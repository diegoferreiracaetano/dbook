package com.dbook.domain

/** Persistence port for [Booking]. */
interface BookingRepository {
    fun findById(id: Long): Booking?

    fun save(booking: Booking): Booking
}
