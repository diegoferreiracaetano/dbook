package com.dbook.domain.catalog

/** Persistence port for [Airport]. */
interface AirportRepository {
    fun findByIataCode(iataCode: String): Airport?

    fun findById(id: Long): Airport?

    fun findAll(): List<Airport>

    /** @throws DuplicateIataCodeException if another airport already has the code. */
    fun save(airport: Airport): Airport

    fun delete(id: Long)

    /** How many flights leave from or arrive at the airport. */
    fun flightCount(id: Long): Long
}
