package com.dbook.domain.catalog

import java.math.BigDecimal
import java.time.LocalDate

/** Persistence port for [Flight]. */
interface FlightRepository {
    fun findById(id: Long): Flight?

    fun save(flight: Flight): Flight

    /**
     * Saves the changes of an existing flight. With [expectedVersion] it only goes through if the flight is still at
     * that version (what the caller last read), otherwise it throws [com.dbook.domain.common.StaleVersionException].
     */
    fun update(
        flight: Flight,
        expectedVersion: Long? = null,
    ): Flight

    /** Public search by route and departure date, used by `GET /flights/search`. */
    fun search(
        originIataCode: String,
        destinationIataCode: String,
        date: LocalDate,
    ): List<Flight>

    /**
     * Lowest price among active flights to [destinationIataCode] departing within
     * [from]..[to] (inclusive), or `null` if none exist — used by
     * `GET /flights/lowest-price`. An aggregate query (`MIN(price)`), not a full scan of
     * matching flights.
     */
    fun findLowestPrice(
        destinationIataCode: String,
        from: LocalDate,
        to: LocalDate,
    ): BigDecimal?

    /**
     * Candidate pool for AI suggestions — bounding/ordering (soonest departures first)
     * is an adapter concern, kept out of this port.
     */
    fun findActive(): List<Flight>
}
