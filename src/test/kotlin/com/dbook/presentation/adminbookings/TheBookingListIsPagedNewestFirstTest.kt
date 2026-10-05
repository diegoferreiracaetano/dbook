package com.dbook.presentation.adminbookings

import kotlin.test.Test
import kotlin.test.assertEquals

class TheBookingListIsPagedNewestFirstTest : AdminBookingsFixture() {
    @Test
    fun `given three bookings when listing in pages of two then newest first with the total`() {
        val email = uniqueEmail()
        val customer = registerAndLogin(email)
        val (first, second, third) = bookSeats(customer, 3)
        val support = staff()
        val id = userIdOf(email).toString()

        val page0 = bodyOf(searchBookings(support, "customerId" to id, "size" to "2"))
        val page1 = bodyOf(searchBookings(support, "customerId" to id, "size" to "2", "page" to "1"))

        assertEquals(listOf(third, second), page0["items"].map { it["id"].asLong() })
        assertEquals(listOf(first), page1["items"].map { it["id"].asLong() })
        assertEquals(3, page0["totalElements"].asInt())
    }
}
