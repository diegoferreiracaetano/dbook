package com.dbook.presentation.catalogadmin

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AFlightWithActiveBookingsCannotBeCancelledTest : CatalogAdminFixture() {
    @Test
    fun `given a flight with a pending booking when cancelling then 409 with how many, then it works`() {
        val token = manager()
        val id = createFlight(token)
        val customer = registerAndLogin(uniqueEmail())
        val booking = book(customer, id, seatIdOf(id, "1A"))

        val refused = cancelFlight(token, id)

        assertEquals(409, refused.response.status)
        assertTrue(json(refused)["error"].asText().contains("1 active bookings"))
        assertEquals("SCHEDULED", json(flight(token, id))["flight"]["status"].asText())

        post(customer, "/v1/bookings/$booking/cancel", emptyMap())

        assertEquals(200, cancelFlight(token, id).response.status)
    }
}
