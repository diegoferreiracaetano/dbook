package com.dbook.domain.promo

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant

enum class PromoType { PERCENT, FIXED }

/** Why a code cannot be used. */
enum class PromoRejection(val explanation: String) {
    INACTIVE("this code is not active"),
    NOT_STARTED("this code is not valid yet"),
    EXPIRED("this code has expired"),
    BELOW_MINIMUM("the total is below the minimum for this code"),
    EXHAUSTED("this code has already been used the maximum number of times"),
    USER_LIMIT_REACHED("you have already used this code the maximum number of times"),
}

class PromoRejectedException(val code: String, val reason: PromoRejection) :
    RuntimeException("Promo code $code cannot be used: ${reason.explanation}")

class PromoNotFoundException(code: String) : RuntimeException("Promo code not found: $code")

class DuplicatePromoCodeException(code: String) : RuntimeException("Promo code $code already exists")

private val CODE_PATTERN = Regex("^[A-Z0-9_-]{3,32}$")
private val ONE_HUNDRED = BigDecimal("100")

/**
 * A code that takes money off a payment. The code, its type and its value never change once created (a different
 * discount is a different code); the team can move the validity window, the minimum and the limits, and switch it off.
 */
data class PromoCode(
    val id: Long? = null,
    val code: String,
    val type: PromoType,
    val value: BigDecimal,
    val minAmount: BigDecimal = BigDecimal.ZERO,
    val validFrom: Instant,
    val validUntil: Instant,
    val maxRedemptions: Int? = null,
    val maxPerUser: Int = 1,
    val redeemed: Int = 0,
    val active: Boolean = true,
    val createdBy: Long,
    val createdAt: Instant,
) {
    init {
        require(CODE_PATTERN.matches(code)) { "code must have 3 to 32 capital letters, digits, '-' or '_'" }
        require(value > BigDecimal.ZERO) { "value must be positive" }
        require(type != PromoType.PERCENT || value < ONE_HUNDRED) { "a percentage must be below 100" }
        require(minAmount >= BigDecimal.ZERO) { "minAmount must not be negative" }
        require(validUntil.isAfter(validFrom)) { "validUntil must be after validFrom" }
        require(maxRedemptions == null || maxRedemptions >= 1) { "maxRedemptions must be at least 1, or none" }
        require(maxPerUser >= 1) { "maxPerUser must be at least 1" }
        require(redeemed >= 0 && (maxRedemptions == null || redeemed <= maxRedemptions)) {
            "maxRedemptions cannot be below what was already redeemed"
        }
    }

    companion object {
        /** The form a code is kept and looked up in: no spaces, in capitals. */
        fun normalize(code: String): String = code.trim().uppercase()
    }

    val isExhausted: Boolean get() = maxRedemptions != null && redeemed >= maxRedemptions

    /**
     * How much this code takes off [amount] (the total of [bookingCount] bookings) at [now]. Never more than the
     * amount minus one cent per booking: a payment is never free, and no booking is left with nothing to refund.
     * A percentage is rounded half-up to the cent. Does not look at how many times it was used: that is decided
     * atomically when the payment is made.
     */
    fun discountFor(
        amount: BigDecimal,
        now: Instant,
        bookingCount: Int = 1,
    ): BigDecimal {
        rejection(amount, now)?.let { throw PromoRejectedException(code, it) }
        // divide() with an explicit scale and mode: the operator would round to the scale of the numerator, half-even
        val raw =
            if (type == PromoType.PERCENT) (amount * value).divide(ONE_HUNDRED, CENTS, RoundingMode.HALF_UP) else value
        return raw.setScale(CENTS, RoundingMode.HALF_UP).min(maxDiscount(amount, bookingCount))
    }

    private fun rejection(
        amount: BigDecimal,
        now: Instant,
    ): PromoRejection? =
        when {
            !active -> PromoRejection.INACTIVE
            now.isBefore(validFrom) -> PromoRejection.NOT_STARTED
            !now.isBefore(validUntil) -> PromoRejection.EXPIRED
            amount < minAmount -> PromoRejection.BELOW_MINIMUM
            else -> null
        }
}

private const val CENTS = 2
