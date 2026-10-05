package com.dbook.infrastructure.persistence.catalog

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface AirlineJpaRepository : JpaRepository<AirlineJpaEntity, Long> {
    fun findByIataCode(iataCode: String): AirlineJpaEntity?

    @Query("SELECT COUNT(f) FROM FlightJpaEntity f WHERE f.airline.id = :id")
    fun countFlights(
        @Param("id") id: Long,
    ): Long
}
