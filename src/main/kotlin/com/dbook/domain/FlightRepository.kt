package com.dbook.domain

import java.time.LocalDate

interface FlightRepository {
    fun findById(id: Long): Flight?

    fun save(flight: Flight): Flight

    fun search(
        originIataCode: String,
        destinationIataCode: String,
        date: LocalDate,
    ): List<Flight>
}
