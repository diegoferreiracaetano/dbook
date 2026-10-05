package com.dbook.presentation.dashboard

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AnEmptyPeriodHasZerosAndNoRatesTest : DashboardFixture() {
    @Test
    fun `given a period with nothing in it when reading the summary then the money is zero and the rates are null`() {
        val start = newWindow()

        val result = summary(staff(), start, start.plusDays(5))

        assertEquals(0.0, result["netRevenue"].asDouble())
        assertEquals(0, result["bookingsByStatus"].size())
        assertTrue(result["conversionRate"].isNull)
        assertTrue(result["expirationRate"].isNull)
        assertTrue(result["averageOccupancy"].isNull)
    }
}
