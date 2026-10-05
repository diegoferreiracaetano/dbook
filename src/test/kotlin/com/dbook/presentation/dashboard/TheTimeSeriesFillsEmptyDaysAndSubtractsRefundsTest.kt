package com.dbook.presentation.dashboard

import kotlin.test.Test
import kotlin.test.assertEquals

class TheTimeSeriesFillsEmptyDaysAndSubtractsRefundsTest : DashboardFixture() {
    @Test
    fun `given revenue and a refund on other days when asking per day then gaps are 0 and the refund negative`() {
        val start = newWindow()
        val customer = customerCreatedAt(at(start))
        val flight = flightOn(start.atTime(8, 0))
        val paid = booking(customer, flight, "100.00", at(start), Triple("CONFIRMED", at(start.plusDays(1)), customer))
        refundCompletedAt(paid, customer, "40.00", at(start.plusDays(3)))
        val token = staff()

        val series =
            json(get(token, "/v1/admin/dashboard/timeseries?metric=REVENUE&from=$start&to=${start.plusDays(4)}"))

        assertEquals(listOf(0.0, 100.0, 0.0, -40.0, 0.0), series["points"].map { it["value"].asDouble() })
        assertEquals(start.toString(), series["points"][0]["date"].asText())
    }
}
