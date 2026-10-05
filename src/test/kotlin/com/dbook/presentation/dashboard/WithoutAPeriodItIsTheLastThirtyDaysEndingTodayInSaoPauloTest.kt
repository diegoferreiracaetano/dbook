package com.dbook.presentation.dashboard

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class WithoutAPeriodItIsTheLastThirtyDaysEndingTodayInSaoPauloTest : DashboardFixture() {
    @Test
    fun `given no period when reading the summary then it covers the last 30 days ending today in Sao Paulo`() {
        val result = json(get(staff(), "/v1/admin/dashboard/summary"))

        val today = LocalDate.now(com.dbook.domain.dashboard.DashboardPeriod.ZONE)
        assertEquals(today.toString(), result["to"].asText())
        assertEquals(today.minusDays(29).toString(), result["from"].asText())
    }
}
