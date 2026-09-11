package com.dbook.domain

/** Reference data seeded via Flyway migrations, resolved by IATA code (e.g. "GRU"). */
class Airport(
    val id: Long? = null,
    val iataCode: String,
    val name: String,
    val city: String,
    val country: String,
)
