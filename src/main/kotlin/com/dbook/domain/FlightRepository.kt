package com.dbook.domain

import java.time.LocalDate

/** Persistence port for [Flight]. */
interface FlightRepository {
    fun findById(id: Long): Flight?

    fun save(flight: Flight): Flight

    /** Public search by route and departure date, used by `GET /flights/search`. */
    fun search(
        originIataCode: String,
        destinationIataCode: String,
        date: LocalDate,
    ): List<Flight>

    /**
     * Candidate pool for AI suggestions — bounding/ordering (soonest departures first)
     * is an adapter concern, kept out of this port.
     */
    fun findActive(): List<Flight>
}
