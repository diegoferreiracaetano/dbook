package com.dbook.presentation.dashboard

import com.dbook.application.dashboard.GetDashboardSummaryUseCase
import com.dbook.application.dashboard.GetTimeSeriesUseCase
import com.dbook.application.dashboard.GetTopRoutesUseCase
import com.dbook.domain.dashboard.DashboardSummary
import com.dbook.domain.dashboard.Granularity
import com.dbook.domain.dashboard.TimeSeries
import com.dbook.domain.dashboard.TimeSeriesMetric
import com.dbook.domain.dashboard.TopRoutes
import com.dbook.presentation.common.ApiPaths
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

/**
 * `/admin/dashboard` — business numbers. `from` and `to` are dates (2026-09-01), both included, counted in São Paulo
 * time; without them, the last 30 days. A period has at most 400 days. Answers are kept for a minute.
 */
@RestController
@RequestMapping("${ApiPaths.V1}/admin/dashboard")
@Tag(name = "Dashboard (admin)", description = "Business numbers: revenue, bookings, customers, occupancy")
@SecurityRequirement(name = "bearerAuth")
class DashboardController(
    private val getDashboardSummaryUseCase: GetDashboardSummaryUseCase,
    private val getTimeSeriesUseCase: GetTimeSeriesUseCase,
    private val getTopRoutesUseCase: GetTopRoutesUseCase,
) {
    @Operation(summary = "The numbers of a period: bookings by status, revenue, customers, conversion, occupancy")
    @PreAuthorize("hasAuthority('DASHBOARD_READ')")
    @GetMapping("/summary")
    fun summary(
        @RequestParam(required = false) from: LocalDate?,
        @RequestParam(required = false) to: LocalDate?,
    ): DashboardSummary = getDashboardSummaryUseCase.execute(from, to)

    @Operation(summary = "One metric over time, a point per day or per week (empty ones are zero)")
    @PreAuthorize("hasAuthority('DASHBOARD_READ')")
    @GetMapping("/timeseries")
    fun timeSeries(
        @RequestParam metric: TimeSeriesMetric,
        @RequestParam(defaultValue = "DAY") granularity: Granularity,
        @RequestParam(required = false) from: LocalDate?,
        @RequestParam(required = false) to: LocalDate?,
    ): TimeSeries = getTimeSeriesUseCase.execute(metric, granularity, from, to)

    @Operation(summary = "The routes with the most bookings paid in the period")
    @PreAuthorize("hasAuthority('DASHBOARD_READ')")
    @GetMapping("/top-routes")
    fun topRoutes(
        @RequestParam(defaultValue = "${GetTopRoutesUseCase.DEFAULT_LIMIT}") limit: Int,
        @RequestParam(required = false) from: LocalDate?,
        @RequestParam(required = false) to: LocalDate?,
    ): TopRoutes = getTopRoutesUseCase.execute(limit, from, to)
}
