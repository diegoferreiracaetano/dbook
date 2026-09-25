package com.dbook.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface ReviewJpaRepository : JpaRepository<ReviewJpaEntity, Long> {
    fun findByBookingId(bookingId: Long): ReviewJpaEntity?
}
