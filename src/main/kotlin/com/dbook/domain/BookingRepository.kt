package com.dbook.domain

interface BookingRepository {
    fun findById(id: Long): Booking?

    fun save(booking: Booking): Booking
}
