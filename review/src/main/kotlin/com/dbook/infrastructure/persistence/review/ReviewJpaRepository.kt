package com.dbook.infrastructure.persistence.review

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ReviewJpaRepository : JpaRepository<ReviewJpaEntity, Long> {
    fun findByBookingId(bookingId: Long): ReviewJpaEntity?

    fun findByBookingIdIn(bookingIds: Collection<Long>): List<ReviewJpaEntity>

    @Query(
        value =
            "SELECT AVG(r.rating) FROM review r " +
                "JOIN booking b ON b.id = r.booking_id " +
                "LEFT JOIN flight f ON f.id = b.bookable_id " +
                "LEFT JOIN accommodation ac ON ac.id = b.bookable_id " +
                "JOIN airport a ON a.id = COALESCE(f.destination_airport_id, ac.destination_airport_id) " +
                "WHERE a.iata_code = :destinationIataCode AND r.status = 'VISIBLE'",
        nativeQuery = true,
    )
    fun findAverageRatingByDestination(
        @Param("destinationIataCode") destinationIataCode: String,
    ): Double?
}
