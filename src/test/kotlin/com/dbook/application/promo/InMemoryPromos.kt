package com.dbook.application.promo

import com.dbook.domain.promo.DuplicatePromoCodeException
import com.dbook.domain.promo.PromoCode
import com.dbook.domain.promo.PromoRepository
import com.dbook.domain.promo.RedeemResult
import java.math.BigDecimal
import java.time.Instant

class InMemoryPromos(vararg initial: PromoCode) : PromoRepository {
    private val promos = initial.mapIndexed { index, promo -> promo.copy(id = index + 1L) }.toMutableList()
    val redemptions = mutableListOf<Triple<Long, Long, Long>>()

    override fun findByCode(code: String): PromoCode? = promos.find { it.code == code }

    override fun findById(id: Long): PromoCode? = promos.find { it.id == id }

    override fun save(promo: PromoCode): PromoCode {
        if (promo.id == null) {
            if (promos.any { it.code == promo.code }) throw DuplicatePromoCodeException(promo.code)
            return promo.copy(id = promos.size + 1L).also { promos += it }
        }
        promos.replaceAll { if (it.id == promo.id) promo else it }
        return promo
    }

    override fun redemptionsBy(
        promoId: Long,
        userId: Long,
    ): Int = redemptions.count { it.first == promoId && it.second == userId }

    override fun redeem(
        promoId: Long,
        userId: Long,
        paymentId: Long,
        discount: BigDecimal,
        now: Instant,
    ): RedeemResult {
        val promo = requireNotNull(findById(promoId))
        return when {
            promo.isExhausted -> RedeemResult.EXHAUSTED
            redemptionsBy(promoId, userId) >= promo.maxPerUser -> RedeemResult.USER_LIMIT_REACHED
            else -> {
                promos.replaceAll { if (it.id == promoId) it.copy(redeemed = it.redeemed + 1) else it }
                redemptions += Triple(promoId, userId, paymentId)
                RedeemResult.REDEEMED
            }
        }
    }
}
