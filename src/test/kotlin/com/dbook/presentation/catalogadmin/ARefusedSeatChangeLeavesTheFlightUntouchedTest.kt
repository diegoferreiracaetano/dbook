package com.dbook.presentation.catalogadmin

import kotlin.test.Test
import kotlin.test.assertEquals

class ARefusedSeatChangeLeavesTheFlightUntouchedTest : CatalogAdminFixture() {
    @Test
    fun `given a booked last seat when shrinking the capacity then it is 409 and nothing changed, price included`() {
        val token = manager()
        val id = createFlight(token, flightBody(capacity = 12))
        val customer = registerAndLogin(uniqueEmail())
        book(customer, id, seatIdOf(id, "2F"))
        val version = versionOf(token, id)

        val result = edit(token, id, mapOf("totalCapacity" to 10, "price" to 999.0))

        assertEquals(409, result.response.status)
        assertEquals(12, seatLabels(id).size)
        assertEquals(100.0, json(flight(token, id))["flight"]["price"].asDouble())
        assertEquals(version, versionOf(token, id))
    }
}
