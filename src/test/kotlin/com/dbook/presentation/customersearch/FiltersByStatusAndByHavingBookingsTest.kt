package com.dbook.presentation.customersearch

import kotlin.test.Test
import kotlin.test.assertEquals

class FiltersByStatusAndByHavingBookingsTest : CustomerSearchFixture() {
    @Test
    fun `given a customer with a booking and a blocked one when filtering then each filter finds the right one`() {
        val tag = newTag()
        val bookerEmail = uniqueEmail()
        val bookerToken = registerAndLogin(bookerEmail, name = "Booker $tag")
        val (bookableId, seatId) = registerFlightWithOneSeat()
        book(bookerToken, bookableId, seatId)
        val blockedEmail = newCustomer("Blocked $tag")
        block(blockedEmail)
        val token = supportToken()

        val withBookings = search(token, "query" to tag, "hasBookings" to "true")
        val blocked = search(token, "query" to tag, "status" to "BLOCKED")
        val withoutBookings = search(token, "query" to tag, "hasBookings" to "false")

        assertEquals(listOf("Booker $tag"), namesOf(withBookings))
        assertEquals(1, bodyOf(withBookings)["items"][0]["bookingCount"].asInt())
        assertEquals(listOf("Blocked $tag"), namesOf(blocked))
        assertEquals(listOf("Blocked $tag"), namesOf(withoutBookings))
    }
}
