package com.dbook.domain.catalog

import java.time.LocalDate

/**
 * A short-lived copy of the public search results, per route and day. The search is the most repeated read of the API
 * and the answer changes only when the catalog does, so it is kept for a few seconds. Whoever changes a flight, an
 * airport or an airline calls [invalidateAll]; seats taken in the meantime show up when the copy expires (a booking
 * is always checked against the real seats, so a copy that is a few seconds old can only make a booking fail with a
 * 409, never succeed wrongly).
 */
interface FlightSearchCache {
    /**
     * The copy for this search, or the result of [load] (kept for next time). A copy that cannot be read, or a cache
     * that is down, just means [load] runs: the cache never makes a search fail.
     */
    fun remember(
        originIataCode: String,
        destinationIataCode: String,
        date: LocalDate,
        load: () -> List<Flight>,
    ): List<Flight>

    /** Every copy is stale from now on. Called inside a transaction, it takes effect when that transaction commits. */
    fun invalidateAll()
}
