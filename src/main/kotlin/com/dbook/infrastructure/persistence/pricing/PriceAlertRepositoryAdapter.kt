package com.dbook.infrastructure.persistence.pricing

import com.dbook.domain.common.PageQuery
import com.dbook.domain.common.PageResult
import com.dbook.domain.pricing.DuplicatePriceAlertException
import com.dbook.domain.pricing.PriceAlert
import com.dbook.domain.pricing.PriceAlertRepository
import com.dbook.domain.pricing.PriceChange
import com.dbook.domain.pricing.TriggeredAlert
import com.dbook.infrastructure.persistence.common.queryPage
import org.springframework.dao.DuplicateKeyException
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate

@Repository
class PriceAlertRepositoryAdapter(
    private val jdbc: NamedParameterJdbcTemplate,
) : PriceAlertRepository {
    // the customer's row is locked first, as in the favorites: counting and inserting cannot be raced
    @Transactional
    override fun create(
        alert: PriceAlert,
        limit: Int,
        today: LocalDate,
    ): PriceAlert? {
        jdbc.queryForList(
            "SELECT id FROM app_user WHERE id = :user FOR UPDATE",
            mapOf("user" to alert.userId),
            Long::class.javaObjectType,
        )
        val active =
            jdbc.queryForObject(
                "SELECT count(*) FROM price_alert WHERE user_id = :user AND active AND travel_date >= :today",
                mapOf("user" to alert.userId, "today" to java.sql.Date.valueOf(today)),
                Int::class.javaObjectType,
            ) ?: 0
        if (active >= limit) {
            return null
        }
        return try {
            val keys = GeneratedKeyHolder()
            jdbc.update(
                "INSERT INTO price_alert (user_id, origin, destination, travel_date, target_price, active, " +
                    "created_at) VALUES (:user, :origin, :destination, :date, :target, :active, :created)",
                params(alert),
                keys,
                arrayOf("id"),
            )
            alert.copy(id = keys.key?.toLong())
        } catch (ex: DuplicateKeyException) {
            throw DuplicatePriceAlertException().also { it.initCause(ex) }
        }
    }

    @Transactional(readOnly = true)
    override fun findById(id: Long): PriceAlert? =
        jdbc.query("SELECT * FROM price_alert WHERE id = :id", mapOf("id" to id)) { rs, _ -> alertOf(rs) }.firstOrNull()

    @Transactional
    override fun save(alert: PriceAlert): PriceAlert {
        jdbc.update(
            "UPDATE price_alert SET target_price = :target, active = :active, last_notified_at = :notified " +
                "WHERE id = :id",
            params(alert).addValue("id", alert.id),
        )
        return requireNotNull(findById(requireNotNull(alert.id)))
    }

    @Transactional
    override fun delete(id: Long) {
        jdbc.update("DELETE FROM price_alert WHERE id = :id", mapOf("id" to id))
    }

    @Transactional(readOnly = true)
    override fun findByUser(
        userId: Long,
        page: PageQuery,
    ): PageResult<PriceAlert> =
        jdbc.queryPage(
            countSql = "SELECT count(*) FROM price_alert WHERE user_id = :user",
            pageSql =
                "SELECT * FROM price_alert WHERE user_id = :user ORDER BY created_at DESC, id DESC " +
                    "LIMIT :limit OFFSET :offset",
            params = MapSqlParameterSource("user", userId),
            page = page,
        ) { alertOf(it) }

    // One statement both finds the alerts and marks them notified, so the claim is the decision
    @Transactional
    override fun claimTriggered(
        change: PriceChange,
        now: Instant,
        notifiedBefore: Instant,
    ): List<TriggeredAlert> =
        jdbc.query(
            "UPDATE price_alert SET last_notified_at = :now " +
                "WHERE active AND origin = :origin AND destination = :destination AND travel_date = :date " +
                "AND target_price >= :price AND (last_notified_at IS NULL OR last_notified_at <= :before) " +
                "RETURNING id, user_id, target_price",
            mapOf(
                "now" to Timestamp.from(now),
                "origin" to change.origin,
                "destination" to change.destination,
                "date" to java.sql.Date.valueOf(change.travelDate),
                "price" to change.price,
                "before" to Timestamp.from(notifiedBefore),
            ),
        ) { rs, _ -> TriggeredAlert(rs.getLong("id"), rs.getLong("user_id"), rs.getBigDecimal("target_price")) }

    private fun params(alert: PriceAlert) =
        MapSqlParameterSource()
            .addValue("user", alert.userId).addValue("origin", alert.origin).addValue("destination", alert.destination)
            .addValue("date", java.sql.Date.valueOf(alert.travelDate)).addValue("target", alert.targetPrice)
            .addValue("active", alert.active).addValue("created", Timestamp.from(alert.createdAt))
            .addValue("notified", alert.lastNotifiedAt?.let { Timestamp.from(it) })

    private fun alertOf(rs: ResultSet) =
        PriceAlert(
            id = rs.getLong("id"),
            userId = rs.getLong("user_id"),
            origin = rs.getString("origin"),
            destination = rs.getString("destination"),
            travelDate = rs.getDate("travel_date").toLocalDate(),
            targetPrice = rs.getBigDecimal("target_price"),
            active = rs.getBoolean("active"),
            lastNotifiedAt = rs.getTimestamp("last_notified_at")?.toInstant(),
            createdAt = rs.getTimestamp("created_at").toInstant(),
        )
}
