package com.dbook.infrastructure.persistence.crm

import com.dbook.domain.crm.CustomerFilter
import com.dbook.domain.crm.CustomerSort
import com.dbook.domain.crm.CustomerSortField
import com.dbook.domain.crm.CustomerSummary
import com.dbook.domain.identity.UserStatus
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import java.sql.ResultSet
import java.sql.Timestamp

// The SQL of "which customers, in what order, as which row", shared by the paged search and the CSV export so
// the two can never disagree about what a filter means.
object CustomerSql {
    const val SELECT_SUMMARY =
        "SELECT u.id, u.name, u.email, u.status, u.created_at, u.last_login_at, " +
            "(SELECT count(*) FROM booking b WHERE b.customer_id = u.id) AS booking_count FROM app_user u"

    // same expression as the V31 indexes, so the planner can use them
    private const val MATCHES_TEXT =
        """(immutable_unaccent(lower(u.name)) LIKE immutable_unaccent(lower(:pattern)) ESCAPE '\'
            OR immutable_unaccent(lower(u.email)) LIKE immutable_unaccent(lower(:pattern)) ESCAPE '\')"""

    private const val HAS_BOOKINGS = "EXISTS (SELECT 1 FROM booking b WHERE b.customer_id = u.id)"

    fun where(
        filter: CustomerFilter,
        params: MapSqlParameterSource,
    ): String {
        val conditions = mutableListOf("u.role = 'CLIENT'")
        filter.searchText?.let {
            params.addValue("pattern", containsPattern(it))
            conditions += MATCHES_TEXT
        }
        filter.status?.let {
            params.addValue("status", it.name)
            conditions += "u.status = :status"
        }
        filter.createdFrom?.let {
            params.addValue("createdFrom", Timestamp.from(it))
            conditions += "u.created_at >= :createdFrom"
        }
        filter.createdTo?.let {
            params.addValue("createdTo", Timestamp.from(it))
            conditions += "u.created_at <= :createdTo"
        }
        filter.hasBookings?.let { conditions += if (it) HAS_BOOKINGS else "NOT $HAS_BOOKINGS" }
        return conditions.joinToString(" AND ")
    }

    // One page. The booking count is a subquery per row, so it must run on the rows of the page and not on the ones
    // an OFFSET skips: the LIMIT is inside, on the plain columns, and the count is added to what is left. Measured
    // on 50 000 customers, a page at offset 40 000 went from 286 ms to a plain index walk.
    fun pageSql(
        where: String,
        sort: CustomerSort,
    ): String =
        """
        SELECT p.id, p.name, p.email, p.status, p.created_at, p.last_login_at,
               (SELECT count(*) FROM booking b WHERE b.customer_id = p.id) AS booking_count
        FROM (SELECT u.id, u.name, u.email, u.status, u.created_at, u.last_login_at
              FROM app_user u WHERE $where ${orderBy(sort, "u")} LIMIT :limit OFFSET :offset) p
        ${orderBy(sort, "p")}
        """

    // the column comes from a closed enum, never from the request, so nothing user-typed reaches the SQL text
    fun orderBy(
        sort: CustomerSort,
        alias: String = "u",
    ): String {
        val column =
            when (sort.field) {
                CustomerSortField.NAME -> "immutable_unaccent(lower($alias.name))"
                CustomerSortField.EMAIL -> "lower($alias.email)"
                CustomerSortField.CREATED_AT -> "$alias.created_at"
                CustomerSortField.LAST_LOGIN_AT -> "$alias.last_login_at"
            }
        // only here: elsewhere NULLS LAST would stop the (created_at DESC, id DESC) index from serving the sort
        val nulls = if (sort.field == CustomerSortField.LAST_LOGIN_AT) " NULLS LAST" else ""
        return "ORDER BY $column ${sort.direction}$nulls, $alias.id ${sort.direction}"
    }

    fun summaryOf(rs: ResultSet) =
        CustomerSummary(
            id = rs.getLong("id"),
            name = rs.getString("name"),
            email = rs.getString("email"),
            status = UserStatus.valueOf(rs.getString("status")),
            createdAt = rs.getTimestamp("created_at").toInstant(),
            lastLoginAt = rs.getTimestamp("last_login_at")?.toInstant(),
            bookingCount = rs.getLong("booking_count"),
        )

    // `\`, `%` and `_` are LIKE wildcards: escaped, they search for the character itself
    private fun containsPattern(text: String): String =
        "%" + text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%"
}
