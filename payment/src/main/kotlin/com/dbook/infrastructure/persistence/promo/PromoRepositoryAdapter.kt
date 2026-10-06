package com.dbook.infrastructure.persistence.promo

import com.dbook.domain.promo.DuplicatePromoCodeException
import com.dbook.domain.promo.PromoCode
import com.dbook.domain.promo.PromoRepository
import com.dbook.domain.promo.PromoType
import com.dbook.domain.promo.RedeemResult
import org.springframework.dao.DuplicateKeyException
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant

@Repository
class PromoRepositoryAdapter(
    private val jdbc: NamedParameterJdbcTemplate,
) : PromoRepository {
    @Transactional(readOnly = true)
    override fun findByCode(code: String): PromoCode? =
        jdbc.query("SELECT * FROM promo_code WHERE code = :code", mapOf("code" to code)) { rs, _ -> promoOf(rs) }
            .firstOrNull()

    @Transactional(readOnly = true)
    override fun findById(id: Long): PromoCode? =
        jdbc.query("SELECT * FROM promo_code WHERE id = :id", mapOf("id" to id)) { rs, _ -> promoOf(rs) }.firstOrNull()

    @Transactional
    override fun save(promo: PromoCode): PromoCode =
        try {
            if (promo.id == null) insert(promo) else update(promo)
        } catch (ex: DuplicateKeyException) {
            throw DuplicatePromoCodeException(promo.code).also { it.initCause(ex) }
        }

    @Transactional(readOnly = true)
    override fun redemptionsBy(
        promoId: Long,
        userId: Long,
    ): Int =
        jdbc.queryForObject(
            "SELECT count(*) FROM promo_redemption WHERE promo_id = :promo AND user_id = :user",
            mapOf("promo" to promoId, "user" to userId),
            Int::class.javaObjectType,
        ) ?: 0

    // The conditional UPDATE is the whole decision: it raises the count only while there is a use left, and the row
    // lock it takes makes every other redemption of the same code wait for this transaction. The per-customer count
    // that follows therefore cannot be raced either.
    @Transactional(propagation = Propagation.MANDATORY)
    override fun redeem(
        promoId: Long,
        userId: Long,
        paymentId: Long,
        discount: BigDecimal,
        now: Instant,
    ): RedeemResult {
        val maxPerUser =
            jdbc.query(
                "UPDATE promo_code SET redeemed = redeemed + 1 " +
                    "WHERE id = :id AND (max_redemptions IS NULL OR redeemed < max_redemptions) RETURNING max_per_user",
                mapOf("id" to promoId),
            ) { rs, _ -> rs.getInt("max_per_user") }.firstOrNull()
        return when {
            maxPerUser == null -> RedeemResult.EXHAUSTED
            redemptionsBy(promoId, userId) >= maxPerUser -> RedeemResult.USER_LIMIT_REACHED
            else -> {
                recordRedemption(promoId, userId, paymentId, discount, now)
                RedeemResult.REDEEMED
            }
        }
    }

    private fun recordRedemption(
        promoId: Long,
        userId: Long,
        paymentId: Long,
        discount: BigDecimal,
        now: Instant,
    ) {
        jdbc.update(
            "INSERT INTO promo_redemption (promo_id, user_id, payment_id, discount, created_at) " +
                "VALUES (:promo, :user, :payment, :discount, :now)",
            mapOf(
                "promo" to promoId,
                "user" to userId,
                "payment" to paymentId,
                "discount" to discount,
                "now" to Timestamp.from(now),
            ),
        )
    }

    private fun insert(promo: PromoCode): PromoCode {
        val keys = GeneratedKeyHolder()
        jdbc.update(
            "INSERT INTO promo_code (code, type, value, min_amount, valid_from, valid_until, max_redemptions, " +
                "max_per_user, active, created_by, created_at) VALUES (:code, :type, :value, :minAmount, :validFrom, " +
                ":validUntil, :maxRedemptions, :maxPerUser, :active, :createdBy, :createdAt)",
            params(promo),
            keys,
            arrayOf("id"),
        )
        return promo.copy(id = keys.key?.toLong())
    }

    // Only what the team may change: never the code, the type or the value
    private fun update(promo: PromoCode): PromoCode {
        jdbc.update(
            "UPDATE promo_code SET min_amount = :minAmount, valid_from = :validFrom, valid_until = :validUntil, " +
                "max_redemptions = :maxRedemptions, max_per_user = :maxPerUser, active = :active WHERE id = :id",
            params(promo).addValue("id", promo.id),
        )
        return requireNotNull(findById(requireNotNull(promo.id)))
    }

    private fun params(promo: PromoCode) =
        MapSqlParameterSource()
            .addValue("code", promo.code).addValue("type", promo.type.name).addValue("value", promo.value)
            .addValue("minAmount", promo.minAmount).addValue("validFrom", Timestamp.from(promo.validFrom))
            .addValue("validUntil", Timestamp.from(promo.validUntil))
            .addValue("maxRedemptions", promo.maxRedemptions).addValue("maxPerUser", promo.maxPerUser)
            .addValue("active", promo.active).addValue("createdBy", promo.createdBy)
            .addValue("createdAt", Timestamp.from(promo.createdAt))

    private fun promoOf(rs: ResultSet) = rs.toPromo()
}

fun ResultSet.toPromo() =
    PromoCode(
        id = getLong("id"),
        code = getString("code"),
        type = PromoType.valueOf(getString("type")),
        value = getBigDecimal("value"),
        minAmount = getBigDecimal("min_amount"),
        validFrom = getTimestamp("valid_from").toInstant(),
        validUntil = getTimestamp("valid_until").toInstant(),
        maxRedemptions = getInt("max_redemptions").takeIf { !wasNull() },
        maxPerUser = getInt("max_per_user"),
        redeemed = getInt("redeemed"),
        active = getBoolean("active"),
        createdBy = getLong("created_by"),
        createdAt = getTimestamp("created_at").toInstant(),
    )
