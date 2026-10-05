package com.dbook.presentation.customerhistory

import kotlin.test.Test
import kotlin.test.assertEquals

class APageBeyondTheEndIsEmptyButKeepsTheTotalTest : CustomerHistoryFixture() {
    @Test
    fun `given one booking when asking for page 5 then the items are empty and the total is still 1`() {
        val email = uniqueEmail()
        bookSeats(registerAndLogin(email), 1)

        val body = bodyOf(history(supportToken(), userIdOf(email), "bookings", "page" to "5"))

        assertEquals(0, body["items"].size())
        assertEquals(1, body["totalElements"].asInt())
    }
}
