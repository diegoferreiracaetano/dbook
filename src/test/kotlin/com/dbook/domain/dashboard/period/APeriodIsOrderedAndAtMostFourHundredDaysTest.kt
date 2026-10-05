package com.dbook.domain.dashboard.period

import com.dbook.domain.dashboard.DashboardPeriod
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class APeriodIsOrderedAndAtMostFourHundredDaysTest {
    private val start = LocalDate.of(2026, 1, 1)

    @Test
    fun `given 400 days when building the period then it is accepted, and 401 or an inverted one is refused`() {
        assertEquals(400, DashboardPeriod(start, start.plusDays(399)).days)
        assertEquals(1, DashboardPeriod(start, start).days)
        assertFailsWith<IllegalArgumentException> { DashboardPeriod(start, start.plusDays(400)) }
        assertFailsWith<IllegalArgumentException> { DashboardPeriod(start, start.minusDays(1)) }
    }

    @Test
    fun `given today when asking for the last 30 days then it ends today and starts 29 days before`() {
        val period = DashboardPeriod.lastDays(30, LocalDate.of(2026, 10, 4))

        assertEquals(LocalDate.of(2026, 9, 5), period.from)
        assertEquals(LocalDate.of(2026, 10, 4), period.to)
    }
}
