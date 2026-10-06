package com.dbook.infrastructure.cache

import kotlin.test.Test
import kotlin.test.assertEquals

// The search results carry each flight's airports. The catalog tells whoever listens that an airport changed
// (AirportChangeListener) and the search cache listens: it does not need the catalog to know it exists.
class ChangingAnAirportMakesTheCachedSearchStaleToTest : FlightSearchCacheFixture() {
    private fun airportBody(
        code: String,
        city: String,
    ) = mapOf(
        "iataCode" to code,
        "name" to "Test Field",
        "city" to city,
        "country" to "Brasil",
        "photoUrl" to "https://example.com/t.jpg",
        "region" to "América do Sul",
        "isPopular" to false,
    )

    @Test
    fun `given a cached search when the origin airport is renamed then the next search shows the new city`() {
        val team = manager()
        // the code is random, so it can collide with an airport another test created: draw again until it is free
        val (code, created) =
            generateSequence { ('A'..'Z').shuffled().take(3).joinToString("") }
                .map { it to post(team, "/v1/admin/airports", airportBody(it, "Testville")) }
                .first { it.second.response.status != 409 }
        val departure = uniqueDeparture()
        createFlight(team, flightBody(departure, origin = code))
        assertEquals("Testville", search.execute(code, "GIG", departure.toLocalDate()).single().origin.city)

        put(team, "/v1/admin/airports/${json(created)["id"].asLong()}", airportBody(code, "New City"))

        assertEquals("New City", search.execute(code, "GIG", departure.toLocalDate()).single().origin.city)
    }
}
