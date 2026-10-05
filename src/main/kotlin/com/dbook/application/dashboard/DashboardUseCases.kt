package com.dbook.application.dashboard

import com.dbook.domain.dashboard.DashboardCache
import com.dbook.domain.dashboard.DashboardPeriod
import com.dbook.domain.dashboard.DashboardReader
import com.dbook.domain.dashboard.DashboardSummary
import com.dbook.domain.dashboard.Granularity
import com.dbook.domain.dashboard.TimeSeries
import com.dbook.domain.dashboard.TimeSeriesMetric
import com.dbook.domain.dashboard.TopRoutes
import io.micrometer.observation.annotation.Observed
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.LocalDate

// Without a period, the last 30 days ending today, São Paulo time.
private fun periodOf(
    from: LocalDate?,
    to: LocalDate?,
    clock: Clock,
): DashboardPeriod {
    val end = to ?: LocalDate.now(clock.withZone(DashboardPeriod.ZONE))
    return DashboardPeriod(from ?: end.minusDays(DashboardPeriod.DEFAULT_DAYS - 1), end)
}

@Observed(name = "dbook.usecase")
@Service
class GetDashboardSummaryUseCase(
    private val dashboardReader: DashboardReader,
    private val dashboardCache: DashboardCache,
    private val clock: Clock,
) {
    fun execute(
        from: LocalDate?,
        to: LocalDate?,
    ): DashboardSummary {
        val period = periodOf(from, to, clock)
        return dashboardCache.remember("summary:${period.from}:${period.to}", DashboardSummary::class.java) {
            dashboardReader.summary(period)
        }
    }
}

@Observed(name = "dbook.usecase")
@Service
class GetTimeSeriesUseCase(
    private val dashboardReader: DashboardReader,
    private val dashboardCache: DashboardCache,
    private val clock: Clock,
) {
    fun execute(
        metric: TimeSeriesMetric,
        granularity: Granularity,
        from: LocalDate?,
        to: LocalDate?,
    ): TimeSeries {
        val period = periodOf(from, to, clock)
        val key = "series:$metric:$granularity:${period.from}:${period.to}"
        return dashboardCache.remember(key, TimeSeries::class.java) {
            dashboardReader.timeSeries(metric, granularity, period)
        }
    }
}

@Observed(name = "dbook.usecase")
@Service
class GetTopRoutesUseCase(
    private val dashboardReader: DashboardReader,
    private val dashboardCache: DashboardCache,
    private val clock: Clock,
) {
    fun execute(
        limit: Int,
        from: LocalDate?,
        to: LocalDate?,
    ): TopRoutes {
        require(limit in 1..MAX_LIMIT) { "limit must be between 1 and $MAX_LIMIT" }
        val period = periodOf(from, to, clock)
        return dashboardCache.remember("routes:$limit:${period.from}:${period.to}", TopRoutes::class.java) {
            dashboardReader.topRoutes(limit, period)
        }
    }

    companion object {
        const val DEFAULT_LIMIT = 10
        const val MAX_LIMIT = 50
    }
}
