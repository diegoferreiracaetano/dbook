package com.dbook.domain.review

interface ReviewRepository {
    fun findById(id: Long): Review?

    fun findByBookingId(bookingId: Long): Review?

    /** The reviews of several bookings in one query, for a list. */
    fun findByBookingIds(bookingIds: Collection<Long>): List<Review> = bookingIds.mapNotNull { findByBookingId(it) }

    fun save(review: Review): Review

    fun delete(id: Long)

    /** The average of the visible reviews of the flights to a destination; null when there are none. */
    fun findAverageRatingByDestination(destinationIataCode: String): Double?
}
