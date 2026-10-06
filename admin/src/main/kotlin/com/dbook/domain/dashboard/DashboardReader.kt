package com.dbook.domain.dashboard

interface DashboardReader {
    fun summary(period: DashboardPeriod): DashboardSummary

    /** One point per bucket of the period, empty buckets included as zero, in order. */
    fun timeSeries(
        metric: TimeSeriesMetric,
        granularity: Granularity,
        period: DashboardPeriod,
    ): TimeSeries

    /** The routes with the most bookings paid in the period, most first. */
    fun topRoutes(
        limit: Int,
        period: DashboardPeriod,
    ): TopRoutes
}
