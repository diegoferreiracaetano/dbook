package com.dbook.domain.promo

import com.dbook.domain.common.PageQuery
import com.dbook.domain.common.PageResult
import java.math.BigDecimal
import java.time.Instant

data class PromoRedemptionRow(
    val userId: Long,
    val userName: String,
    val paymentId: Long,
    val discount: BigDecimal,
    val createdAt: Instant,
)

interface AdminPromoReader {
    /** Newest first; [active] narrows to the ones switched on or off. */
    fun search(
        active: Boolean?,
        page: PageQuery,
    ): PageResult<PromoCode>

    /** Who used the code, newest first. */
    fun redemptions(
        promoId: Long,
        page: PageQuery,
    ): PageResult<PromoRedemptionRow>
}
