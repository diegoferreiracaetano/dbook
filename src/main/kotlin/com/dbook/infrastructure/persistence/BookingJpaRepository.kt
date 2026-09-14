package com.dbook.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface BookingJpaRepository : JpaRepository<BookingJpaEntity, Long> {
    fun findByCustomerId(customerId: Long): List<BookingJpaEntity>
}
