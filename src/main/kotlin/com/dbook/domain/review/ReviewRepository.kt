package com.dbook.domain.review

interface ReviewRepository {
    fun findById(id: Long): Review?

    fun findByBookingId(bookingId: Long): Review?

    fun save(review: Review): Review

    fun findAverageRatingByDestination(destinationIataCode: String): Double?
}
