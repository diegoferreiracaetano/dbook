package com.dbook.domain.payment

import java.math.BigDecimal

private val CARD_LAST4_PATTERN = Regex("^\\d{4}$")
private const val MAX_IDEMPOTENCY_KEY_LENGTH = 64

/**
 * A completed payment covering one or more [Booking]s. Only the last 4 digits of the
 * card and the cardholder name are kept — there's no real payment gateway behind this,
 * so the full card number/CVV never leave the client and have nothing to be stored for.
 */
class Payment(
    val id: Long? = null,
    val customerId: Long,
    val amount: BigDecimal,
    val cardLast4: String,
    val cardholderName: String,
    val idempotencyKey: String? = null,
    val requestFingerprint: String? = null,
) {
    init {
        require(amount > BigDecimal.ZERO) { "amount must be positive" }
        require(CARD_LAST4_PATTERN.matches(cardLast4)) { "cardLast4 must be exactly 4 digits" }
        require(cardholderName.isNotBlank()) { "cardholderName must not be blank" }
        require(
            idempotencyKey == null ||
                (idempotencyKey.isNotBlank() && idempotencyKey.length <= MAX_IDEMPOTENCY_KEY_LENGTH),
        ) {
            "idempotencyKey must have 1 to $MAX_IDEMPOTENCY_KEY_LENGTH characters"
        }
    }
}
