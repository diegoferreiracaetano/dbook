package com.dbook.domain.dashboard

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * The days a dashboard number covers, both ends included, **counted in São Paulo time**: a booking paid at 23:30 on
 * the 30th there belongs to the 30th, even though it is already the 31st in UTC. Capped at 400 days.
 */
data class DashboardPeriod(
    val from: LocalDate,
    val to: LocalDate,
) {
    init {
        require(!to.isBefore(from)) { "to must not be before from" }
        require(days <= MAX_DAYS) { "a period has at most $MAX_DAYS days" }
    }

    val days: Long get() = to.toEpochDay() - from.toEpochDay() + 1

    /** The first instant of [from]. */
    val start: Instant get() = from.atStartOfDay(ZONE).toInstant()

    /** The first instant after [to]: the period is `[start, endExclusive)`. */
    val endExclusive: Instant get() = to.plusDays(1).atStartOfDay(ZONE).toInstant()

    companion object {
        const val MAX_DAYS = 400L
        const val DEFAULT_DAYS = 30L
        val ZONE: ZoneId = ZoneId.of("America/Sao_Paulo")

        /** The last [days] days, ending on [today]. */
        fun lastDays(
            days: Long,
            today: LocalDate,
        ) = DashboardPeriod(today.minusDays(days - 1), today)
    }
}
