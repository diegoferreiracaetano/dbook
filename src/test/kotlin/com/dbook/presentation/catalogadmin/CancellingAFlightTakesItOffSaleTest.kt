package com.dbook.presentation.catalogadmin

import org.springframework.test.web.servlet.get
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class CancellingAFlightTakesItOffSaleTest : CatalogAdminFixture() {
    @Test
    fun `given a flight without bookings when cancelled then it leaves the search and cannot be booked or edited`() {
        val token = manager()
        val departure = uniqueDeparture()
        val id = createFlight(token, flightBody(departure = departure))
        val search = {
            mockMvc.get("/v1/flights/search?origin=GRU&destination=GIG&date=${departure.toLocalDate()}").andReturn()
        }
        assertEquals(true, json(search()).any { it["id"].asLong() == id })

        val cancelled = cancelFlight(token, id)

        assertEquals(200, cancelled.response.status)
        assertEquals("CANCELLED", json(cancelled)["flight"]["status"].asText())
        assertFalse(json(search()).any { it["id"].asLong() == id })
        assertEquals(409, tryBook(registerAndLogin(uniqueEmail()), id, seatIdOf(id, "1A")).response.status)
        assertEquals(409, edit(token, id, mapOf("price" to 10.0)).response.status)
        assertEquals(409, cancelFlight(token, id).response.status)
    }
}
