package com.dbook.presentation.customerhistory

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class PaymentsShowTheLastFourDigitsAndTheBookingsTheyCoverTest : CustomerHistoryFixture() {
    @Test
    fun `given two payments when listing then newest first with the booking each covers and no cardholder name`() {
        val email = uniqueEmail()
        val token = registerAndLogin(email)
        val (first, second) = bookSeats(token, 2)
        pay(token, first)
        pay(token, second)

        val result = history(supportToken(), userIdOf(email), "payments")

        val items = bodyOf(result)["items"]
        assertEquals(listOf(listOf(second), listOf(first)), items.map { p -> p["bookingIds"].map { it.asLong() } })
        assertEquals("4242", items[0]["cardLast4"].asText())
        assertEquals(100.0, items[0]["amount"].asDouble())
        assertFalse(bodyOf(result).toString().contains("Jane Doe"))
    }
}
