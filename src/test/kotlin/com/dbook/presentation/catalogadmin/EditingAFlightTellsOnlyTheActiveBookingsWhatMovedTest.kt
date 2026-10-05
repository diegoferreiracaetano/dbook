package com.dbook.presentation.catalogadmin

import org.springframework.test.web.servlet.post
import kotlin.test.Test
import kotlin.test.assertEquals

class EditingAFlightTellsOnlyTheActiveBookingsWhatMovedTest : CatalogAdminFixture() {
    private fun changedEvents(flightId: Long): List<String> =
        jdbcTemplate.queryForList(
            "SELECT payload::text FROM outbox_event WHERE type = 'flight.changed' AND aggregate_id = ?",
            String::class.java,
            flightId.toString(),
        )

    @Test
    fun `given two bookings, one cancelled, when the departure moves then only the active one gets an event`() {
        val token = manager()
        val departure = uniqueDeparture()
        val flight = createFlight(token, flightBody(departure))
        val active = registerAndLogin(uniqueEmail())
        val cancelling = registerAndLogin(uniqueEmail())
        book(active, flight, seatIdOf(flight, "1A"))
        val cancelled = book(cancelling, flight, seatIdOf(flight, "1B"))
        mockMvc.post("/v1/bookings/$cancelled/cancel") { header("Authorization", "Bearer $cancelling") }

        edit(
            token,
            flight,
            mapOf(
                "departureTime" to departure.plusHours(2).toString(),
                "arrivalTime" to departure.plusHours(3).toString(),
            ),
        )

        val events = changedEvents(flight).map { objectMapper.readTree(it) }
        assertEquals(1, events.size)
        assertEquals(listOf("departureTime", "arrivalTime"), events[0]["changes"].map { it.asText() })
    }

    @Test
    fun `given a booking when only the price changes then no event is written`() {
        val token = manager()
        val flight = createFlight(token)
        book(registerAndLogin(uniqueEmail()), flight, seatIdOf(flight, "1A"))

        edit(token, flight, mapOf("price" to 250.0))

        assertEquals(0, changedEvents(flight).size)
    }
}
