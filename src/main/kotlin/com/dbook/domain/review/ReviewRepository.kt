package com.dbook.domain.review

interface ReviewRepository {
    fun findById(id: Long): Review?

    fun findByBookingId(bookingId: Long): Review?

    fun save(review: Review): Review

    fun delete(id: Long)

    /** The average of the visible reviews of the flights to a destination; null when there are none. */
    fun findAverageRatingByDestination(destinationIataCode: String): Double?
}
