package com.dbook.domain.promo

import java.math.BigDecimal
import java.time.Instant

enum class RedeemResult { REDEEMED, EXHAUSTED, USER_LIMIT_REACHED }

interface PromoRepository {
    fun findByCode(code: String): PromoCode?

    fun findById(id: Long): PromoCode?

    /** Creates (no id) or changes (with id) a code. @throws DuplicatePromoCodeException if the code exists. */
    fun save(promo: PromoCode): PromoCode

    /** How many times [userId] used the code. */
    fun redemptionsBy(
        promoId: Long,
        userId: Long,
    ): Int

    /**
     * Uses the code for a payment, **atomically**: the count is raised in one conditional statement, never read and
     * then written, so two payments racing for the last use cannot both win. It joins the transaction of the payment
     * (and fails outside one): when the caller throws on anything but [RedeemResult.REDEEMED] the whole payment,
     * and the use, are undone.
     */
    fun redeem(
        promoId: Long,
        userId: Long,
        paymentId: Long,
        discount: BigDecimal,
        now: Instant,
    ): RedeemResult
}
