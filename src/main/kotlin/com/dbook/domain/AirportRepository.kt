package com.dbook.domain

/** Persistence port for [Airport], a reference/lookup table seeded via Flyway migrations. */
interface AirportRepository {
    fun findByIataCode(iataCode: String): Airport?
}
