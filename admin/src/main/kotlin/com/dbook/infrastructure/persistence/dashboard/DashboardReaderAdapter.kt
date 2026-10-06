package com.dbook.infrastructure.persistence.dashboard

import com.dbook.domain.dashboard.DashboardPeriod
import com.dbook.domain.dashboard.DashboardReader
import com.dbook.domain.dashboard.DashboardSummary
import com.dbook.domain.dashboard.Granularity
import com.dbook.domain.dashboard.TimeSeries
import com.dbook.domain.dashboard.TimeSeriesMetric
import com.dbook.domain.dashboard.TimeSeriesPoint
import com.dbook.domain.dashboard.TopRoute
import com.dbook.domain.dashboard.TopRoutes
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.sql.Timestamp
import java.time.LocalDate

// Plain SQL aggregates over the tables the numbers come from: nothing is stored or kept in step (see DashboardSql).
@Repository
class DashboardReaderAdapter(
    private val jdbc: NamedParameterJdbcTemplate,
) : DashboardReader {
    @Transactional(readOnly = true)
    override fun summary(period: DashboardPeriod): DashboardSummary {
        val params = params(period)
        val gross = money(DashboardSql.GROSS_REVENUE, params)
        val refunded = money(DashboardSql.REFUNDED, params)
        val created = count(DashboardSql.CREATED_COUNT, params)
        return DashboardSummary(
            from = period.from,
            to = period.to,
            bookingsByStatus = bookingsByStatus(params),
            grossRevenue = gross,
            refunded = refunded,
            netRevenue = gross - refunded,
            newCustomers = count(DashboardSql.NEW_CUSTOMERS, params),
            conversionRate = rate(count(DashboardSql.PAID_COUNT, params), created),
            expirationRate = rate(count(DashboardSql.EXPIRED_COUNT, params), created),
            averageOccupancy = occupancy(period),
        )
    }

    @Transactional(readOnly = true)
    override fun timeSeries(
        metric: TimeSeriesMetric,
        granularity: Granularity,
        period: DashboardPeriod,
    ): TimeSeries {
        val params = params(period).addValue("unit", granularity.name.lowercase())
        val found = mutableMapOf<LocalDate, BigDecimal>()
        jdbc.query(DashboardSql.series(metric), params) { rs ->
            found[rs.getDate("bucket").toLocalDate()] = rs.getBigDecimal("value")
        }
        val points = bucketsOf(period, granularity).map { TimeSeriesPoint(it, found[it] ?: BigDecimal.ZERO) }
        return TimeSeries(metric, granularity, points)
    }

    @Transactional(readOnly = true)
    override fun topRoutes(
        limit: Int,
        period: DashboardPeriod,
    ): TopRoutes =
        TopRoutes(
            limit,
            jdbc.query(DashboardSql.TOP_ROUTES, params(period).addValue("limit", limit)) { rs, _ ->
                TopRoute(
                    rs.getString("origin"),
                    rs.getString("destination"),
                    rs.getLong("bookings"),
                    rs.getBigDecimal("revenue"),
                )
            },
        )

    private fun bookingsByStatus(params: MapSqlParameterSource): Map<String, Long> {
        val byStatus = sortedMapOf<String, Long>()
        jdbc.query(DashboardSql.BY_STATUS, params) { rs -> byStatus[rs.getString("status")] = rs.getLong("n") }
        return byStatus
    }

    // flights are on local (São Paulo) time already, so the dates of the period compare directly
    private fun occupancy(period: DashboardPeriod): Double? {
        val params =
            MapSqlParameterSource()
                .addValue("from", Timestamp.valueOf(period.from.atStartOfDay()))
                .addValue("to", Timestamp.valueOf(period.to.plusDays(1).atStartOfDay()))
        var reserved = 0L
        var capacity = 0L
        jdbc.query(DashboardSql.OCCUPANCY, params) { rs ->
            reserved = rs.getLong("reserved")
            capacity = rs.getLong("capacity")
        }
        return rate(reserved, capacity)
    }

    private fun params(period: DashboardPeriod) =
        MapSqlParameterSource()
            .addValue("start", Timestamp.from(period.start))
            .addValue("end", Timestamp.from(period.endExclusive))

    private fun money(
        sql: String,
        params: MapSqlParameterSource,
    ): BigDecimal = jdbc.queryForObject(sql, params, BigDecimal::class.java) ?: BigDecimal.ZERO

    private fun count(
        sql: String,
        params: MapSqlParameterSource,
    ): Long = jdbc.queryForObject(sql, params, Long::class.javaObjectType) ?: 0L

    private fun rate(
        part: Long,
        whole: Long,
    ): Double? =
        if (whole == 0L) {
            null
        } else {
            BigDecimal(part).divide(BigDecimal(whole), RATE_SCALE, RoundingMode.HALF_UP).toDouble()
        }

    private companion object {
        const val RATE_SCALE = 4
    }
}
