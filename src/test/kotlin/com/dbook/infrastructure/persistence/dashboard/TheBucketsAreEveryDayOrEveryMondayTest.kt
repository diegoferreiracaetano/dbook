package com.dbook.infrastructure.persistence.dashboard

import com.dbook.domain.dashboard.DashboardPeriod
import com.dbook.domain.dashboard.Granularity
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class TheBucketsAreEveryDayOrEveryMondayTest {
    private val period = DashboardPeriod(LocalDate.of(2026, 9, 2), LocalDate.of(2026, 9, 17)) // Wed .. Thu

    @Test
    fun `given a period when bucketed by day then every day is there, in order`() {
        val days = bucketsOf(period, Granularity.DAY)

        assertEquals(16, days.size)
        assertEquals(LocalDate.of(2026, 9, 2), days.first())
        assertEquals(LocalDate.of(2026, 9, 17), days.last())
    }

    @Test
    fun `given a period starting on a Wednesday when bucketed by week then the first bucket is the Monday before`() {
        assertEquals(
            listOf(LocalDate.of(2026, 8, 31), LocalDate.of(2026, 9, 7), LocalDate.of(2026, 9, 14)),
            bucketsOf(period, Granularity.WEEK),
        )
    }
}
