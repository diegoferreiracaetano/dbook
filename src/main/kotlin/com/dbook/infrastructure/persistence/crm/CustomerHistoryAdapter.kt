package com.dbook.infrastructure.persistence.crm

import com.dbook.domain.common.PageQuery
import com.dbook.domain.common.PageResult
import com.dbook.domain.crm.CustomerBooking
import com.dbook.domain.crm.CustomerHistory
import com.dbook.domain.crm.CustomerPayment
import com.dbook.domain.crm.CustomerReview
import com.dbook.infrastructure.persistence.common.queryPage
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.sql.ResultSet
import java.time.LocalDateTime

@Repository
class CustomerHistoryAdapter(
    private val jdbc: NamedParameterJdbcTemplate,
) : CustomerHistory {
    @Transactional(readOnly = true)
    override fun customerExists(id: Long): Boolean =
        jdbc.queryForObject(
            "SELECT EXISTS (SELECT 1 FROM app_user WHERE id = :id AND role = 'CLIENT')",
            mapOf("id" to id),
            Boolean::class.javaObjectType,
        ) == true

    @Transactional(readOnly = true)
    override fun bookings(
        customerId: Long,
        page: PageQuery,
    ): PageResult<CustomerBooking> =
        jdbc.queryPage(
            "SELECT count(*) FROM booking WHERE customer_id = :id",
            BOOKINGS_SQL,
            MapSqlParameterSource("id", customerId),
            page,
            ::bookingOf,
        )

    @Transactional(readOnly = true)
    override fun payments(
        customerId: Long,
        page: PageQuery,
    ): PageResult<CustomerPayment> =
        jdbc.queryPage(
            "SELECT count(*) FROM payment WHERE customer_id = :id",
            PAYMENTS_SQL,
            MapSqlParameterSource("id", customerId),
            page,
            ::paymentOf,
        )

    @Transactional(readOnly = true)
    override fun reviews(
        customerId: Long,
        page: PageQuery,
    ): PageResult<CustomerReview> =
        jdbc.queryPage(
            "SELECT count(*) FROM review WHERE customer_id = :id",
            REVIEWS_SQL,
            MapSqlParameterSource("id", customerId),
            page,
            ::reviewOf,
        )

    private fun bookingOf(rs: ResultSet) =
        CustomerBooking(
            id = rs.getLong("id"),
            status = rs.getString("status"),
            price = rs.getBigDecimal("price"),
            title = rs.getString("title"),
            seatLabel = rs.getString("seat_label"),
            flightNumber = rs.getString("flight_number"),
            origin = rs.getString("origin"),
            destination = rs.getString("destination"),
            departureTime = rs.getObject("departure_time", LocalDateTime::class.java),
            paymentId = rs.getObject("payment_id", Long::class.javaObjectType),
        )

    private fun paymentOf(rs: ResultSet) =
        CustomerPayment(
            id = rs.getLong("id"),
            amount = rs.getBigDecimal("amount"),
            cardLast4 = rs.getString("card_last4"),
            createdAt = rs.getObject("created_at", LocalDateTime::class.java),
            bookingIds = (rs.getArray("booking_ids").array as Array<*>).map { (it as Number).toLong() },
        )

    private fun reviewOf(rs: ResultSet) =
        CustomerReview(
            id = rs.getLong("id"),
            bookingId = rs.getLong("booking_id"),
            rating = rs.getInt("rating"),
            comment = rs.getString("comment"),
            createdAt = rs.getObject("created_at", LocalDateTime::class.java),
        )

    private companion object {
        // flight and airports are LEFT joins: a booking that is not a flight must not disappear from the history
        const val BOOKINGS_SQL = """
            SELECT b.id, b.status, b.price, b.payment_id, bk.title, s.label AS seat_label,
                   f.flight_number, o.iata_code AS origin, d.iata_code AS destination, f.departure_time
            FROM booking b
            JOIN bookable bk ON bk.id = b.bookable_id
            LEFT JOIN seat s ON s.id = b.seat_id
            LEFT JOIN flight f ON f.id = b.bookable_id
            LEFT JOIN airport o ON o.id = f.origin_airport_id
            LEFT JOIN airport d ON d.id = f.destination_airport_id
            WHERE b.customer_id = :id
            ORDER BY b.id DESC
            LIMIT :limit OFFSET :offset
        """

        // the booking ids come from a subquery narrowed by customer, which idx_booking_customer serves
        const val PAYMENTS_SQL = """
            SELECT p.id, p.amount, p.card_last4, p.created_at,
                   coalesce(
                       (SELECT array_agg(b.id ORDER BY b.id) FROM booking b
                        WHERE b.customer_id = p.customer_id AND b.payment_id = p.id),
                       ARRAY[]::bigint[]) AS booking_ids
            FROM payment p
            WHERE p.customer_id = :id
            ORDER BY p.id DESC
            LIMIT :limit OFFSET :offset
        """

        const val REVIEWS_SQL = """
            SELECT id, booking_id, rating, comment, created_at
            FROM review
            WHERE customer_id = :id
            ORDER BY id DESC
            LIMIT :limit OFFSET :offset
        """
    }
}
