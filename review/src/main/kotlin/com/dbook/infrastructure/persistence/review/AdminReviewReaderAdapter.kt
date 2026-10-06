package com.dbook.infrastructure.persistence.review

import com.dbook.domain.common.PageQuery
import com.dbook.domain.common.PageResult
import com.dbook.domain.review.AdminReviewReader
import com.dbook.domain.review.AdminReviewStatus
import com.dbook.domain.review.AdminReviewSummary
import com.dbook.domain.review.ReviewStatus
import com.dbook.infrastructure.persistence.common.queryPage
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class AdminReviewReaderAdapter(
    private val jdbc: NamedParameterJdbcTemplate,
) : AdminReviewReader {
    @Transactional(readOnly = true)
    override fun search(
        status: AdminReviewStatus?,
        page: PageQuery,
    ): PageResult<AdminReviewSummary> {
        val where =
            when (status) {
                AdminReviewStatus.REPORTED -> "r.status = 'VISIBLE' AND EXISTS ($OPEN_REPORT)"
                AdminReviewStatus.HIDDEN -> "r.status = 'HIDDEN'"
                AdminReviewStatus.VISIBLE -> "r.status = 'VISIBLE'"
                null -> "TRUE"
            }
        // oldest report first in the queue: whoever waited longest is seen first; otherwise newest review first
        val order = if (status == AdminReviewStatus.REPORTED) "r.id" else "r.created_at DESC, r.id DESC"
        return jdbc.queryPage(
            countSql = "SELECT count(*) FROM review r WHERE $where",
            pageSql =
                "SELECT p.*, u.name AS customer_name, a.iata_code AS destination, " +
                    "(SELECT count(*) FROM review_report x WHERE x.review_id = p.id AND x.resolved_at IS NULL) " +
                    "AS open_reports, " +
                    "(SELECT x.reason FROM review_report x WHERE x.review_id = p.id " +
                    "ORDER BY x.created_at DESC, x.id DESC LIMIT 1) AS last_report " +
                    "FROM (SELECT r.* FROM review r WHERE $where ORDER BY $order LIMIT :limit OFFSET :offset) p " +
                    "JOIN app_user u ON u.id = p.customer_id JOIN booking b ON b.id = p.booking_id " +
                    "JOIN flight f ON f.id = b.bookable_id JOIN airport a ON a.id = f.destination_airport_id " +
                    "ORDER BY ${order.replace("r.", "p.")}",
            params = MapSqlParameterSource(),
            page = page,
        ) { rs ->
            AdminReviewSummary(
                id = rs.getLong("id"),
                bookingId = rs.getLong("booking_id"),
                customerId = rs.getLong("customer_id"),
                customerName = rs.getString("customer_name"),
                destination = rs.getString("destination"),
                rating = rs.getInt("rating"),
                comment = rs.getString("comment"),
                status = ReviewStatus.valueOf(rs.getString("status")),
                hiddenReason = rs.getString("hidden_reason"),
                openReports = rs.getInt("open_reports"),
                lastReportReason = rs.getString("last_report"),
                createdAt = rs.getTimestamp("created_at").toLocalDateTime(),
            )
        }
    }

    private companion object {
        const val OPEN_REPORT = "SELECT 1 FROM review_report x WHERE x.review_id = r.id AND x.resolved_at IS NULL"
    }
}
