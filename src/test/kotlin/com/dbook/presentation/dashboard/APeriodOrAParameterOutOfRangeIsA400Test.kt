package com.dbook.presentation.dashboard

import kotlin.test.Test
import kotlin.test.assertEquals

class APeriodOrAParameterOutOfRangeIsA400Test : DashboardFixture() {
    @Test
    fun `given 401 days, an inverted period, a bad limit, metric or date when asking then 400`() {
        val token = staff()

        listOf(
            "/v1/admin/dashboard/summary?from=2026-01-01&to=2027-02-05",
            "/v1/admin/dashboard/summary?from=2026-02-01&to=2026-01-01",
            "/v1/admin/dashboard/top-routes?limit=0",
            "/v1/admin/dashboard/top-routes?limit=51",
            "/v1/admin/dashboard/timeseries?metric=PROFIT",
            "/v1/admin/dashboard/timeseries",
            "/v1/admin/dashboard/summary?from=yesterday",
        ).forEach { url -> assertEquals(400, get(token, url).response.status, url) }
        assertEquals(200, get(token, "/v1/admin/dashboard/summary?from=2026-01-01&to=2027-02-04").response.status)
    }
}
