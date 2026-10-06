package com.dbook.infrastructure.persistence.promo

import com.dbook.domain.common.PageQuery
import com.dbook.domain.common.PageResult
import com.dbook.domain.promo.AdminPromoReader
import com.dbook.domain.promo.PromoCode
import com.dbook.domain.promo.PromoRedemptionRow
import com.dbook.infrastructure.persistence.common.queryPage
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class AdminPromoReaderAdapter(
    private val jdbc: NamedParameterJdbcTemplate,
) : AdminPromoReader {
    @Transactional(readOnly = true)
    override fun search(
        active: Boolean?,
        page: PageQuery,
    ): PageResult<PromoCode> {
        val params = MapSqlParameterSource()
        val where =
            if (active == null) {
                "TRUE"
            } else {
                params.addValue("active", active)
                "active = :active"
            }
        return jdbc.queryPage(
            countSql = "SELECT count(*) FROM promo_code WHERE $where",
            pageSql =
                "SELECT * FROM promo_code WHERE $where ORDER BY created_at DESC, id DESC LIMIT :limit OFFSET :offset",
            params = params,
            page = page,
        ) { it.toPromo() }
    }

    @Transactional(readOnly = true)
    override fun redemptions(
        promoId: Long,
        page: PageQuery,
    ): PageResult<PromoRedemptionRow> =
        jdbc.queryPage(
            countSql = "SELECT count(*) FROM promo_redemption WHERE promo_id = :promo",
            pageSql =
                "SELECT r.user_id, u.name, r.payment_id, r.discount, r.created_at FROM promo_redemption r " +
                    "JOIN app_user u ON u.id = r.user_id WHERE r.promo_id = :promo " +
                    "ORDER BY r.created_at DESC, r.id DESC LIMIT :limit OFFSET :offset",
            params = MapSqlParameterSource("promo", promoId),
            page = page,
        ) { rs ->
            PromoRedemptionRow(
                userId = rs.getLong("user_id"),
                userName = rs.getString("name"),
                paymentId = rs.getLong("payment_id"),
                discount = rs.getBigDecimal("discount"),
                createdAt = rs.getTimestamp("created_at").toInstant(),
            )
        }
}
