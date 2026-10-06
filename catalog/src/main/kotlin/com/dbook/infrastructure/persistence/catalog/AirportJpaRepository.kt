package com.dbook.infrastructure.persistence.catalog

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface AirportJpaRepository : JpaRepository<AirportJpaEntity, Long> {
    fun findByIataCode(iataCode: String): AirportJpaEntity?

    @Query("SELECT COUNT(f) FROM FlightJpaEntity f WHERE f.origin.id = :id OR f.destination.id = :id")
    fun countFlights(
        @Param("id") id: Long,
    ): Long
}
