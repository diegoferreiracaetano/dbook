package com.dbook.domain

/** Persistence port for [Airline], a reference/lookup table seeded via Flyway migrations. */
interface AirlineRepository {
    fun findByIataCode(iataCode: String): Airline?
}
