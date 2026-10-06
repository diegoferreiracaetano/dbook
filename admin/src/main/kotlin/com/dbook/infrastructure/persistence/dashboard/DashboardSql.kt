package com.dbook.infrastructure.persistence.dashboard

import com.dbook.domain.dashboard.TimeSeriesMetric

// The SQL of every dashboard number, kept apart from the code that runs it. A booking's "paid" moment is its CONFIRMED
// entry in booking_status_history, which is also what makes a refund count in the period it was completed and not in
// the one of the booking. Periods are `[start, end)` instants (São Paulo midnight to midnight).
object DashboardSql {
    const val ZONE = "America/Sao_Paulo"

    private const val CREATED = "b.created_at >= :start AND b.created_at < :end"

    private const val PAID =
        "FROM booking_status_history h JOIN booking b ON b.id = h.booking_id " +
            "WHERE h.to_status = 'CONFIRMED' AND h.occurred_at >= :start AND h.occurred_at < :end"

    private const val WAS_PAID =
        "EXISTS (SELECT 1 FROM booking_status_history h WHERE h.booking_id = b.id AND h.to_status = 'CONFIRMED')"

    // the expiry job has no authenticated user, so its cancellations are the ones with no actor
    private const val WAS_EXPIRED =
        "EXISTS (SELECT 1 FROM booking_status_history h " +
            "WHERE h.booking_id = b.id AND h.to_status = 'CANCELLED' AND h.actor_id IS NULL)"

    const val GROSS_REVENUE = "SELECT coalesce(sum(b.price), 0) $PAID"

    const val REFUNDED =
        "SELECT coalesce(sum(amount), 0) FROM refund WHERE status = 'COMPLETED' " +
            "AND completed_at >= :start AND completed_at < :end"

    const val CREATED_COUNT = "SELECT count(*) FROM booking b WHERE $CREATED"

    const val PAID_COUNT = "SELECT count(*) FROM booking b WHERE $CREATED AND $WAS_PAID"

    const val EXPIRED_COUNT = "SELECT count(*) FROM booking b WHERE $CREATED AND $WAS_EXPIRED"

    const val BY_STATUS = "SELECT b.status, count(*) AS n FROM booking b WHERE $CREATED GROUP BY b.status"

    const val NEW_CUSTOMERS =
        "SELECT count(*) FROM app_user WHERE role = 'CLIENT' AND created_at >= :start AND created_at < :end"

    const val OCCUPANCY = """
        SELECT coalesce(sum(r.reserved), 0) AS reserved, coalesce(sum(b.total_capacity), 0) AS capacity
        FROM flight f
        JOIN bookable b ON b.id = f.id
        LEFT JOIN LATERAL (
            SELECT count(*) AS reserved FROM seat s WHERE s.bookable_id = f.id AND s.status = 'RESERVED') r ON TRUE
        WHERE b.active AND f.departure_time >= :from AND f.departure_time < :to
    """

    const val TOP_ROUTES = """
        SELECT o.iata_code AS origin, d.iata_code AS destination, count(*) AS bookings, sum(b.price) AS revenue
        FROM booking_status_history h
        JOIN booking b ON b.id = h.booking_id
        JOIN flight f ON f.id = b.bookable_id
        JOIN airport o ON o.id = f.origin_airport_id
        JOIN airport d ON d.id = f.destination_airport_id
        WHERE h.to_status = 'CONFIRMED' AND h.occurred_at >= :start AND h.occurred_at < :end
        GROUP BY o.iata_code, d.iata_code
        ORDER BY count(*) DESC, sum(b.price) DESC, o.iata_code, d.iata_code
        LIMIT :limit
    """

    // refunds enter as negative movements, so the series is net revenue, the same as the summary
    private const val REVENUE_SERIES = """
        SELECT bucket, sum(amount) AS value FROM (
            SELECT date_trunc(:unit, h.occurred_at AT TIME ZONE '$ZONE')::date AS bucket, b.price AS amount
            FROM booking_status_history h JOIN booking b ON b.id = h.booking_id
            WHERE h.to_status = 'CONFIRMED' AND h.occurred_at >= :start AND h.occurred_at < :end
            UNION ALL
            SELECT date_trunc(:unit, completed_at AT TIME ZONE '$ZONE')::date, -amount
            FROM refund WHERE status = 'COMPLETED' AND completed_at >= :start AND completed_at < :end
        ) movements GROUP BY bucket
    """

    fun series(metric: TimeSeriesMetric): String =
        when (metric) {
            TimeSeriesMetric.REVENUE -> REVENUE_SERIES
            TimeSeriesMetric.BOOKINGS ->
                "SELECT date_trunc(:unit, b.created_at AT TIME ZONE '$ZONE')::date AS bucket, count(*) AS value " +
                    "FROM booking b WHERE $CREATED GROUP BY bucket"
            TimeSeriesMetric.NEW_CUSTOMERS ->
                "SELECT date_trunc(:unit, created_at AT TIME ZONE '$ZONE')::date AS bucket, count(*) AS value " +
                    "FROM app_user WHERE role = 'CLIENT' AND created_at >= :start AND created_at < :end GROUP BY bucket"
        }
}
