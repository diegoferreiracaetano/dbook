package com.dbook.infrastructure.persistence.booking

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface BookingJpaRepository : JpaRepository<BookingJpaEntity, Long> {
    // the flight (or hotel), the seat and the payment come with the booking, not one query each
    @EntityGraph(attributePaths = ["bookable", "seat", "payment"])
    fun findByCustomerId(customerId: Long): List<BookingJpaEntity>

    // 'PENDING' is a literal on purpose, not a parameter: PostgreSQL only picks the partial index
    // idx_booking_pending (V26) when the predicate is spelled out in the query text.
    @Query("SELECT COUNT(*) FROM booking WHERE status = 'PENDING'", nativeQuery = true)
    fun countPending(): Long
}
