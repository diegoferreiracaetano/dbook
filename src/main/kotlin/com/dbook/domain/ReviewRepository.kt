package com.dbook.domain

interface ReviewRepository {
    fun findById(id: Long): Review?

    fun findByBookingId(bookingId: Long): Review?

    fun save(review: Review): Review
}
