package com.dbook.infrastructure.persistence.booking

import com.dbook.domain.booking.AdminBookingDetail
import com.dbook.domain.booking.AdminBookingFilter
import com.dbook.domain.booking.AdminBookingReader
import com.dbook.domain.booking.AdminBookingSummary
import com.dbook.domain.booking.BookingPaymentInfo
import com.dbook.domain.booking.BookingRefundInfo
import com.dbook.domain.booking.BookingStatus
import com.dbook.domain.booking.BookingTimelineEntry
import com.dbook.domain.common.PageQuery
import com.dbook.domain.common.PageResult
import com.dbook.infrastructure.persistence.common.queryPage
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.LocalDateTime

@Repository
class AdminBookingReaderAdapter(
    private val jdbc: NamedParameterJdbcTemplate,
) : AdminBookingReader {
    @Transactional(readOnly = true)
    override fun search(
        filter: AdminBookingFilter,
        page: PageQuery,
    ): PageResult<AdminBookingSummary> {
        val params = MapSqlParameterSource()
        val where = whereClause(filter, params)
        return jdbc.queryPage(
            countSql = "SELECT count(*) FROM booking b WHERE $where",
            pageSql = "$SELECT_SUMMARY WHERE $where ORDER BY b.created_at DESC, b.id DESC LIMIT :limit OFFSET :offset",
            params = params,
            page = page,
            mapper = ::summaryOf,
        )
    }

    @Transactional(readOnly = true)
    override fun find(id: Long): AdminBookingDetail? {
        val booking =
            jdbc.query("$SELECT_SUMMARY WHERE b.id = :id", mapOf("id" to id)) { rs, _ -> summaryOf(rs) }.firstOrNull()
                ?: return null
        return AdminBookingDetail(booking, paymentOf(id), refundOf(id), timelineOf(id))
    }

    private fun whereClause(
        filter: AdminBookingFilter,
        params: MapSqlParameterSource,
    ): String {
        val conditions = mutableListOf("TRUE")
        filter.status?.let {
            params.addValue("status", it.name)
            conditions += "b.status = :status"
        }
        filter.bookableId?.let {
            params.addValue("bookableId", it)
            conditions += "b.bookable_id = :bookableId"
        }
        filter.customerId?.let {
            params.addValue("customerId", it)
            conditions += "b.customer_id = :customerId"
        }
        filter.createdFrom?.let {
            params.addValue("createdFrom", Timestamp.from(it))
            conditions += "b.created_at >= :createdFrom"
        }
        filter.createdTo?.let {
            params.addValue("createdTo", Timestamp.from(it))
            conditions += "b.created_at <= :createdTo"
        }
        filter.paid?.let { conditions += if (it) "b.payment_id IS NOT NULL" else "b.payment_id IS NULL" }
        return conditions.joinToString(" AND ")
    }

    private fun summaryOf(rs: ResultSet) =
        AdminBookingSummary(
            id = rs.getLong("id"),
            status = BookingStatus.valueOf(rs.getString("status")),
            price = rs.getBigDecimal("price"),
            discount = rs.getBigDecimal("discount"),
            createdAt = rs.getTimestamp("created_at").toInstant(),
            customerId = rs.getLong("customer_id"),
            customerName = rs.getString("customer_name"),
            bookableId = rs.getLong("bookable_id"),
            title = rs.getString("title"),
            seatLabel = rs.getString("seat_label"),
            checkIn = rs.getDate("check_in")?.toLocalDate(),
            checkOut = rs.getDate("check_out")?.toLocalDate(),
            flightNumber = rs.getString("flight_number"),
            origin = rs.getString("origin"),
            destination = rs.getString("destination"),
            departureTime = rs.getObject("departure_time", LocalDateTime::class.java),
            paymentId = rs.getObject("payment_id", Long::class.javaObjectType),
        )

    private fun paymentOf(bookingId: Long): BookingPaymentInfo? =
        jdbc.query(
            "SELECT p.id, p.amount, p.card_last4, p.created_at FROM payment p " +
                "JOIN booking b ON b.payment_id = p.id WHERE b.id = :id",
            mapOf("id" to bookingId),
        ) { rs, _ ->
            BookingPaymentInfo(
                rs.getLong("id"),
                rs.getBigDecimal("amount"),
                rs.getString("card_last4"),
                rs.getObject("created_at", LocalDateTime::class.java),
            )
        }.firstOrNull()

    // the live refund if there is one, otherwise the most recent failed attempt
    private fun refundOf(bookingId: Long): BookingRefundInfo? =
        jdbc.query(
            "SELECT id, status, amount, reason FROM refund WHERE booking_id = :id " +
                "ORDER BY (status <> 'FAILED') DESC, id DESC LIMIT 1",
            mapOf("id" to bookingId),
        ) { rs, _ ->
            BookingRefundInfo(
                rs.getLong("id"),
                rs.getString("status"),
                rs.getBigDecimal("amount"),
                rs.getString("reason"),
            )
        }.firstOrNull()

    private fun timelineOf(bookingId: Long): List<BookingTimelineEntry> =
        jdbc.query(
            "SELECT from_status, to_status, actor_id, occurred_at FROM booking_status_history " +
                "WHERE booking_id = :id ORDER BY occurred_at, id",
            mapOf("id" to bookingId),
        ) { rs, _ ->
            BookingTimelineEntry(
                from = rs.getString("from_status")?.let(BookingStatus::valueOf),
                to = BookingStatus.valueOf(rs.getString("to_status")),
                actorId = rs.getObject("actor_id", Long::class.javaObjectType),
                occurredAt = rs.getTimestamp("occurred_at").toInstant(),
            )
        }

    private companion object {
        // seat, flight and airports are LEFT joins: a hotel stay (no seat, no flight) must not disappear from the list
        const val SELECT_SUMMARY = """
            SELECT b.id, b.status, b.price, b.discount, b.created_at, b.customer_id, u.name AS customer_name,
                   b.bookable_id,
                   bk.title, s.label AS seat_label, b.payment_id, b.check_in, b.check_out,
                   f.flight_number, o.iata_code AS origin, d.iata_code AS destination, f.departure_time
            FROM booking b
            JOIN app_user u ON u.id = b.customer_id
            JOIN bookable bk ON bk.id = b.bookable_id
            LEFT JOIN seat s ON s.id = b.seat_id
            LEFT JOIN flight f ON f.id = b.bookable_id
            LEFT JOIN airport o ON o.id = f.origin_airport_id
            LEFT JOIN airport d ON d.id = f.destination_airport_id
        """
    }
}
