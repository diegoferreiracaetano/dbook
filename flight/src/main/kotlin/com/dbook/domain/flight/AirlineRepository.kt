package com.dbook.domain.flight

import com.dbook.domain.catalog.DuplicateIataCodeException

/** Persistence port for [Airline]. */
interface AirlineRepository {
    fun findByIataCode(iataCode: String): Airline?

    fun findById(id: Long): Airline?

    fun findAll(): List<Airline>

    /** @throws DuplicateIataCodeException if another airline already has the code. */
    fun save(airline: Airline): Airline

    fun delete(id: Long)

    /** How many flights use the airline. */
    fun flightCount(id: Long): Long
}
