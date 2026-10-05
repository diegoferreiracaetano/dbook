package com.dbook.presentation.adminbookings

import kotlin.test.Test
import kotlin.test.assertEquals

class TheBookingListFiltersByStatusCustomerAndPaymentTest : AdminBookingsFixture() {
    @Test
    fun `given paid, pending and refunded bookings when filtering then each filter finds the right ones`() {
        val email = uniqueEmail()
        val customer = registerAndLogin(email)
        val (pending, paid, refunded) = bookSeats(customer, 3)
        pay(customer, paid)
        pay(customer, refunded)
        val support = staff()
        refund(support, refunded)
        val id = userIdOf(email).toString()

        fun ids(vararg extra: Pair<String, String>) =
            bodyOf(searchBookings(support, "customerId" to id, *extra))["items"].map { it["id"].asLong() }.toSet()

        assertEquals(setOf(pending, paid, refunded), ids())
        assertEquals(setOf(pending), ids("status" to "PENDING"))
        assertEquals(setOf(paid), ids("status" to "CONFIRMED"))
        assertEquals(setOf(refunded), ids("status" to "REFUNDED"))
        assertEquals(setOf(pending), ids("paid" to "false"))
        assertEquals(setOf(paid, refunded), ids("paid" to "true"))
        assertEquals(emptySet(), ids("createdFrom" to "2999-01-01T00:00:00Z"))
    }
}
