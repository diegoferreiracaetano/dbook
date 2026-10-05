package com.dbook.presentation.promo

import com.dbook.application.promo.PromoPreview
import com.dbook.domain.promo.PromoCode
import com.dbook.domain.promo.PromoRedemptionRow
import com.dbook.domain.promo.PromoType
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.Instant

data class ValidatePromoRequest(
    @get:Schema(example = "WELCOME10", description = "the code, in any case")
    val code: String,
    @get:Schema(example = "[1, 2]", description = "the caller's own PENDING bookings it would be applied to")
    val bookingIds: List<Long>,
)

data class PromoPreviewResponse(
    val code: String,
    val type: PromoType,
    val subtotal: BigDecimal,
    val discount: BigDecimal,
    val total: BigDecimal,
) {
    companion object {
        fun from(preview: PromoPreview) =
            PromoPreviewResponse(preview.code, preview.type, preview.subtotal, preview.discount, preview.total)
    }
}

data class CreatePromoRequest(
    @get:Schema(example = "WELCOME10", description = "3 to 32 letters, digits, '-' or '_'; kept in capitals")
    val code: String,
    @get:Schema(example = "PERCENT")
    val type: PromoType,
    @get:Schema(example = "10", description = "percent (below 100) or an amount of money")
    val value: BigDecimal,
    @get:Schema(example = "200.00", description = "the total must be at least this much")
    val minAmount: BigDecimal? = null,
    val validFrom: Instant,
    val validUntil: Instant,
    @get:Schema(example = "100", description = "how many times in all; leave out for no limit")
    val maxRedemptions: Int? = null,
    @get:Schema(example = "1", description = "how many times each customer; 1 if left out")
    val maxPerUser: Int? = null,
)

data class UpdatePromoRequest(
    val minAmount: BigDecimal? = null,
    val validFrom: Instant,
    val validUntil: Instant,
    val maxRedemptions: Int? = null,
    val maxPerUser: Int? = null,
)

data class PromoResponse(
    val id: Long?,
    val code: String,
    val type: PromoType,
    val value: BigDecimal,
    val minAmount: BigDecimal,
    val validFrom: Instant,
    val validUntil: Instant,
    val maxRedemptions: Int?,
    val maxPerUser: Int,
    val redeemed: Int,
    val active: Boolean,
    val createdAt: Instant,
) {
    companion object {
        fun from(promo: PromoCode) =
            PromoResponse(
                promo.id, promo.code, promo.type, promo.value, promo.minAmount, promo.validFrom, promo.validUntil,
                promo.maxRedemptions, promo.maxPerUser, promo.redeemed, promo.active, promo.createdAt,
            )
    }
}

data class PromoRedemptionResponse(
    val userId: Long,
    val userName: String,
    val paymentId: Long,
    val discount: BigDecimal,
    val createdAt: Instant,
) {
    companion object {
        fun from(row: PromoRedemptionRow) =
            PromoRedemptionResponse(row.userId, row.userName, row.paymentId, row.discount, row.createdAt)
    }
}
