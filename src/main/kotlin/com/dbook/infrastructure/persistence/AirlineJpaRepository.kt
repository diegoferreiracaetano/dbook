package com.dbook.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface AirlineJpaRepository : JpaRepository<AirlineJpaEntity, Long> {
    fun findByIataCode(iataCode: String): AirlineJpaEntity?
}
