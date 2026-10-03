package com.dbook.domain.catalog

/** Reference data seeded via Flyway migrations, resolved by IATA code (e.g. "LA"). */
class Airline(
    val id: Long? = null,
    val iataCode: String,
    val name: String,
)
