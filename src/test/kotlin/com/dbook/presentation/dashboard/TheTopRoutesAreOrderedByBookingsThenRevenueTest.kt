package com.dbook.presentation.dashboard

import kotlin.test.Test
import kotlin.test.assertEquals

class TheTopRoutesAreOrderedByBookingsThenRevenueTest : DashboardFixture() {
    @Test
    fun `given paid bookings on three routes when asking for the top ones then most bookings first, a limit cuts`() {
        val start = newWindow()
        val customer = customerCreatedAt(at(start))
        val gig = flightOn(start.atTime(8, 0), origin = "GRU", destination = "GIG")
        val jfk = flightOn(start.atTime(9, 0), origin = "GRU", destination = "JFK")
        val lis = flightOn(start.atTime(10, 0), origin = "GRU", destination = "LIS")

        fun paid(
            flight: Long,
            price: String,
        ) = booking(customer, flight, price, at(start), Triple("CONFIRMED", at(start.plusDays(1)), customer))
        paid(gig, "100.00")
        paid(gig, "100.00")
        paid(jfk, "900.00")
        paid(lis, "500.00")
        booking(customer, lis, "999.00", at(start))
        val token = staff()
        val query = "from=$start&to=${start.plusDays(3)}"

        val all = json(get(token, "/v1/admin/dashboard/top-routes?$query"))["routes"]
        val one = json(get(token, "/v1/admin/dashboard/top-routes?limit=1&$query"))["routes"]

        assertEquals(listOf("GIG", "JFK", "LIS"), all.map { it["destination"].asText() })
        assertEquals(2, all[0]["bookings"].asInt())
        assertEquals(200.0, all[0]["revenue"].asDouble())
        assertEquals(1, one.size())
    }
}
