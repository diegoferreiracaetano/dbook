package com.dbook.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ReviewJpaRepository : JpaRepository<ReviewJpaEntity, Long> {
    fun findByBookingId(bookingId: Long): ReviewJpaEntity?

    @Query(
        value =
            "SELECT AVG(r.rating) FROM review r " +
                "JOIN booking b ON b.id = r.booking_id " +
                "JOIN flight f ON f.id = b.bookable_id " +
                "JOIN airport a ON a.id = f.destination_airport_id " +
                "WHERE a.iata_code = :destinationIataCode",
        nativeQuery = true,
    )
    fun findAverageRatingByDestination(
        @Param("destinationIataCode") destinationIataCode: String,
    ): Double?
}
