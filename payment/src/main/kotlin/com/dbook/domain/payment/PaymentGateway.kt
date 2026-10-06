package com.dbook.domain.payment

import java.math.BigDecimal

/** The processor that actually moves the money. [idempotencyKey] makes calling it twice for one refund safe. */
interface PaymentGateway {
    /** @throws PaymentGatewayException if the money did not move. */
    fun refund(
        paymentId: Long,
        amount: BigDecimal,
        idempotencyKey: String,
    )
}

class PaymentGatewayException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
