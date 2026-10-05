package com.dbook.domain.dashboard.period

import com.dbook.domain.dashboard.DashboardPeriod
import java.time.Instant
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class ThePeriodIsCountedInSaoPauloTimeTest {
    @Test
    fun `given a period when its instants are read then it starts and ends at Sao Paulo midnight`() {
        val period = DashboardPeriod(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30))

        assertEquals(Instant.parse("2026-09-01T03:00:00Z"), period.start)
        assertEquals(Instant.parse("2026-10-01T03:00:00Z"), period.endExclusive)
        assertEquals(30, period.days)
    }
}
