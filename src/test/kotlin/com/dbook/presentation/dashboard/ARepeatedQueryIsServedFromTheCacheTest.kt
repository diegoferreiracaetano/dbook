package com.dbook.presentation.dashboard

import kotlin.test.Test
import kotlin.test.assertEquals

class ARepeatedQueryIsServedFromTheCacheTest : DashboardFixture() {
    @Test
    fun `given a summary read twice when the data changes in between then the second answer is the cached first`() {
        val start = newWindow()
        val customer = customerCreatedAt(at(start))
        val flight = flightOn(start.atTime(8, 0))
        val token = staff()
        val hitsBefore = cacheCount("hit")
        val first = summary(token, start, start.plusDays(5))["grossRevenue"].asDouble()

        booking(customer, flight, "80.00", at(start), Triple("CONFIRMED", at(start.plusDays(1)), customer))
        val second = summary(token, start, start.plusDays(5))["grossRevenue"].asDouble()

        assertEquals(0.0, first)
        assertEquals(0.0, second)
        assertEquals(hitsBefore + 1, cacheCount("hit"))

        clearTheCache()
        assertEquals(80.0, summary(token, start, start.plusDays(5))["grossRevenue"].asDouble())
    }
}
