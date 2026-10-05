package com.dbook.domain.dashboard

import java.math.BigDecimal
import java.time.LocalDate

/**
 * What the numbers mean (see docs/dashboard.md):
 * - revenue: the price of the bookings paid in the period, minus the refunds completed in it;
 * - conversion: of the bookings created in the period, how many were ever paid;
 * - expiration: of the bookings created in the period, how many the system cancelled for lack of payment;
 * - occupancy: the seats reserved on the flights leaving in the period, over their capacity.
 * A rate is null when there is nothing to divide by.
 */
data class DashboardSummary(
    val from: LocalDate,
    val to: LocalDate,
    val bookingsByStatus: Map<String, Long>,
    val grossRevenue: BigDecimal,
    val refunded: BigDecimal,
    val netRevenue: BigDecimal,
    val newCustomers: Long,
    val conversionRate: Double?,
    val expirationRate: Double?,
    val averageOccupancy: Double?,
)

enum class TimeSeriesMetric { REVENUE, BOOKINGS, NEW_CUSTOMERS }

enum class Granularity { DAY, WEEK }

/** [date] is the first day of the bucket: the day itself, or the Monday of the week. */
data class TimeSeriesPoint(
    val date: LocalDate,
    val value: BigDecimal,
)

data class TimeSeries(
    val metric: TimeSeriesMetric,
    val granularity: Granularity,
    val points: List<TimeSeriesPoint>,
)

data class TopRoute(
    val origin: String,
    val destination: String,
    val bookings: Long,
    val revenue: BigDecimal,
)

// [limit] is what was asked for: it also keeps this a multi-field type, which JSON reads back without a creator
data class TopRoutes(
    val limit: Int,
    val routes: List<TopRoute>,
)
