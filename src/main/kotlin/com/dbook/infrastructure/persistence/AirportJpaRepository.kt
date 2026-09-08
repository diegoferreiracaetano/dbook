package com.dbook.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface AirportJpaRepository : JpaRepository<AirportJpaEntity, Long> {
	fun findByIataCode(iataCode: String): AirportJpaEntity?
}
