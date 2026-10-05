package com.dbook.presentation.dashboard

import kotlin.test.Test
import kotlin.test.assertEquals

class ADayEndsAtMidnightInSaoPauloNotInUtcTest : DashboardFixture() {
    @Test
    fun `given payments around Sao Paulo midnight when reading each day then each lands on its own day`() {
        val start = newWindow()
        val customer = customerCreatedAt(at(start))
        val flight = flightOn(start.atTime(8, 0))
        val lastDay = start.plusDays(1)
        // 02:30Z on the 3rd is 23:30 on the 2nd in Sao Paulo; 03:00Z on the 3rd is midnight, already the 3rd
        booking(customer, flight, "70.00", at(start), Triple("CONFIRMED", at(start.plusDays(2), "02:30:00"), customer))
        booking(customer, flight, "30.00", at(start), Triple("CONFIRMED", at(start.plusDays(2), "03:00:00"), customer))
        val token = staff()

        assertEquals(70.0, summary(token, lastDay, lastDay)["grossRevenue"].asDouble())
        assertEquals(30.0, summary(token, start.plusDays(2), start.plusDays(2))["grossRevenue"].asDouble())
        assertEquals(100.0, summary(token, start, start.plusDays(3))["grossRevenue"].asDouble())
    }
}
