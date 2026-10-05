package com.dbook.infrastructure.persistence.dashboard

import com.dbook.domain.dashboard.DashboardPeriod
import com.dbook.domain.dashboard.Granularity
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

private const val WEEK_DAYS = 7L

/**
 * Every bucket of the period, in order: each day, or each Monday (the first one may be before the period starts, like
 * Postgres' `date_trunc('week')`). The query only returns the buckets that have something; the rest are zeros.
 */
internal fun bucketsOf(
    period: DashboardPeriod,
    granularity: Granularity,
): List<LocalDate> {
    val weekly = granularity == Granularity.WEEK
    val first = if (weekly) period.from.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) else period.from
    return generateSequence(first) { it.plusDays(if (weekly) WEEK_DAYS else 1L) }
        .takeWhile { !it.isAfter(period.to) }
        .toList()
}
