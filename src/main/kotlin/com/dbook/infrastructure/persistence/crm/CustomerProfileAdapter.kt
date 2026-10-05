package com.dbook.infrastructure.persistence.crm

import com.dbook.domain.crm.BookingTotals
import com.dbook.domain.crm.CustomerProfile
import com.dbook.domain.crm.CustomerProfileReader
import com.dbook.domain.crm.PaymentTotals
import com.dbook.domain.crm.ReviewTotals
import com.dbook.domain.identity.UserStatus
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.sql.ResultSet

@Repository
class CustomerProfileAdapter(
    private val jdbc: NamedParameterJdbcTemplate,
) : CustomerProfileReader {
    @Transactional(readOnly = true)
    override fun find(id: Long): CustomerProfile? =
        jdbc.query(PROFILE_SQL, mapOf("id" to id)) { rs, _ -> profileOf(rs) }.firstOrNull()

    private fun profileOf(rs: ResultSet) =
        CustomerProfile(
            id = rs.getLong("id"),
            name = rs.getString("name"),
            email = rs.getString("email"),
            status = UserStatus.valueOf(rs.getString("status")),
            blockedReason = rs.getString("blocked_reason"),
            blockedAt = rs.getTimestamp("blocked_at")?.toInstant(),
            createdAt = rs.getTimestamp("created_at").toInstant(),
            lastLoginAt = rs.getTimestamp("last_login_at")?.toInstant(),
            anonymizedAt = rs.getTimestamp("anonymized_at")?.toInstant(),
            bookings =
                BookingTotals(
                    pending = rs.getLong("pending_bookings"),
                    confirmed = rs.getLong("confirmed_bookings"),
                    cancelled = rs.getLong("cancelled_bookings"),
                ),
            payments = PaymentTotals(rs.getLong("payment_count"), rs.getBigDecimal("total_paid")),
            reviews = ReviewTotals(rs.getLong("review_count"), rs.getBigDecimal("average_rating")?.toDouble()),
        )

    private companion object {
        // an aggregate without GROUP BY always returns exactly one row, so the lateral joins never drop the customer
        const val PROFILE_SQL = """
            SELECT u.id, u.name, u.email, u.status, u.blocked_reason, u.blocked_at, u.created_at, u.last_login_at, u.anonymized_at,
                   b.pending AS pending_bookings, b.confirmed AS confirmed_bookings, b.cancelled AS cancelled_bookings,
                   p.total AS payment_count, p.paid AS total_paid,
                   r.total AS review_count, r.average AS average_rating
            FROM app_user u
            CROSS JOIN LATERAL (
                SELECT count(*) FILTER (WHERE status = 'PENDING') AS pending,
                       count(*) FILTER (WHERE status = 'CONFIRMED') AS confirmed,
                       count(*) FILTER (WHERE status = 'CANCELLED') AS cancelled
                FROM booking WHERE customer_id = u.id) b
            CROSS JOIN LATERAL (
                SELECT count(*) AS total, coalesce(sum(amount), 0) AS paid
                FROM payment WHERE customer_id = u.id) p
            CROSS JOIN LATERAL (
                SELECT count(*) AS total, round(avg(rating), 1) AS average
                FROM review WHERE customer_id = u.id) r
            WHERE u.id = :id AND u.role = 'CLIENT'
        """
    }
}
