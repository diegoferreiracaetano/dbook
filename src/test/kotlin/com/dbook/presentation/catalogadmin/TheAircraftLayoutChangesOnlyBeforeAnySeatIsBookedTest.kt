package com.dbook.presentation.catalogadmin

import kotlin.test.Test
import kotlin.test.assertEquals

class TheAircraftLayoutChangesOnlyBeforeAnySeatIsBookedTest : CatalogAdminFixture() {
    @Test
    fun `given a flight without bookings when the aircraft changes then it is rebuilt, with a booking it is 409`() {
        val token = manager()
        val free = createFlight(token, flightBody(capacity = 8))
        val booked = createFlight(token, flightBody(capacity = 8))
        book(registerAndLogin(uniqueEmail()), booked, seatIdOf(booked, "1A"))

        val rebuilt = edit(token, free, mapOf("aircraftType" to "Embraer E195"))
        val refused = edit(token, booked, mapOf("aircraftType" to "Embraer E195"))

        assertEquals(200, rebuilt.response.status)
        assertEquals(listOf("1A", "1B", "1C", "1D", "2A", "2B", "2C", "2D"), seatLabels(free))
        assertEquals(409, refused.response.status)
        assertEquals("Airbus A320", json(flight(token, booked))["flight"]["aircraftType"].asText())
    }
}
