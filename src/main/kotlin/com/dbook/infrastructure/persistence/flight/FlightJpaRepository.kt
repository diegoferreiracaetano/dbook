package com.dbook.infrastructure.persistence.flight

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.math.BigDecimal
import java.time.LocalDateTime

interface FlightJpaRepository : JpaRepository<FlightJpaEntity, Long> {
    // the airline and the two airports come in the same query: without the graph each flight costs three more
    @EntityGraph(attributePaths = ["airline", "origin", "destination"])
    @Suppress("FunctionName")
    fun findByOrigin_IataCodeAndDestination_IataCodeAndDepartureTimeBetweenAndActiveTrue(
        originIataCode: String,
        destinationIataCode: String,
        start: LocalDateTime,
        end: LocalDateTime,
    ): List<FlightJpaEntity>

    @EntityGraph(attributePaths = ["airline", "origin", "destination"])
    fun findTop50ByActiveTrueOrderByDepartureTimeAsc(): List<FlightJpaEntity>

    @Query(
        "SELECT MIN(f.price) FROM FlightJpaEntity f " +
            "WHERE f.destination.iataCode = :destinationIataCode " +
            "AND f.active = true " +
            "AND f.departureTime BETWEEN :start AND :end",
    )
    fun findLowestPrice(
        @Param("destinationIataCode") destinationIataCode: String,
        @Param("start") start: LocalDateTime,
        @Param("end") end: LocalDateTime,
    ): BigDecimal?
}
