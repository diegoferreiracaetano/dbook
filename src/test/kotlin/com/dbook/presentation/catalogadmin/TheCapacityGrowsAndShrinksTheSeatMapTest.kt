package com.dbook.presentation.catalogadmin

import kotlin.test.Test
import kotlin.test.assertEquals

class TheCapacityGrowsAndShrinksTheSeatMapTest : CatalogAdminFixture() {
    @Test
    fun `given a flight of 12 seats when the capacity becomes 14 and then 8 then the seat map follows`() {
        val token = manager()
        val id = createFlight(token, flightBody(capacity = 12))

        edit(token, id, mapOf("totalCapacity" to 14))
        assertEquals(14, seatLabels(id).size)
        assertEquals(setOf("3A", "3B"), seatLabels(id).takeLast(2).toSet())

        edit(token, id, mapOf("totalCapacity" to 8))
        assertEquals(8, seatLabels(id).size)
        assertEquals(8, json(flight(token, id))["flight"]["availableSeats"].asInt())
    }
}
