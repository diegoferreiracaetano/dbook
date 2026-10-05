package com.dbook.presentation.customerprofile

import kotlin.test.Test
import kotlin.test.assertEquals

class TheProfileHoldsTheTotalsOfTheCustomersHistoryTest : CustomerProfileFixture() {
    @Test
    fun `given a customer with a pending, a paid and a cancelled booking when reading the profile then totals match`() {
        val email = uniqueEmail()
        val token = registerAndLogin(email)
        val (_, paid, cancelled) = bookSeats(token, 3)
        pay(token, paid)
        cancel(token, cancelled)
        review(token, paid, rating = 4)

        val body = bodyOf(profile(supportToken(), userIdOf(email)))

        assertEquals(email, body["email"].asText())
        assertEquals(
            listOf(3L, 1L, 1L, 1L),
            listOf("total", "pending", "confirmed", "cancelled").map {
                body["bookings"][it].asLong()
            },
        )
        assertEquals(1, body["payments"]["count"].asInt())
        assertEquals(100.0, body["payments"]["totalPaid"].asDouble())
        assertEquals(1, body["reviews"]["count"].asInt())
        assertEquals(4.0, body["reviews"]["averageRating"].asDouble())
    }
}
