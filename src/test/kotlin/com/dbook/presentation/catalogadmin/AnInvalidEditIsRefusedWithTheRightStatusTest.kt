package com.dbook.presentation.catalogadmin

import kotlin.test.Test
import kotlin.test.assertEquals

class AnInvalidEditIsRefusedWithTheRightStatusTest : CatalogAdminFixture() {
    @Test
    fun `given a bad arrival, the same airport twice or an unknown airline when editing then 400 and 404`() {
        val token = manager()
        val id = createFlight(token)
        val departure = json(flight(token, id))["flight"]["departureTime"].asText()

        assertEquals(400, edit(token, id, mapOf("arrivalTime" to departure)).response.status)
        assertEquals(400, edit(token, id, mapOf("destinationIataCode" to "GRU")).response.status)
        assertEquals(404, edit(token, id, mapOf("airlineIataCode" to "ZZ")).response.status)
        assertEquals(404, editBlind(token, 999_999_999L).response.status)
    }
}
