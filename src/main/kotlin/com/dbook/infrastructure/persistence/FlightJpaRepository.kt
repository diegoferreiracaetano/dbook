package com.dbook.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime

interface FlightJpaRepository : JpaRepository<FlightJpaEntity, Long> {
	fun findByOrigin_IataCodeAndDestination_IataCodeAndDepartureTimeBetween(
		originIataCode: String,
		destinationIataCode: String,
		start: LocalDateTime,
		end: LocalDateTime,
	): List<FlightJpaEntity>
}
