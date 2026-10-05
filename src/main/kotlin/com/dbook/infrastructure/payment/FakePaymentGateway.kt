package com.dbook.infrastructure.payment

import com.dbook.domain.payment.PaymentGateway
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.math.BigDecimal

/**
 * Stands in for a real payment processor: it always succeeds. There is no real gateway behind this project (see
 * [com.dbook.domain.payment.Payment]), so nothing here talks to a network.
 *
 * A real adapter would call the processor's refund endpoint with [idempotencyKey] as its idempotency header (so a
 * retry after a timeout cannot refund twice), turn a decline or a timeout into a
 * [com.dbook.domain.payment.PaymentGatewayException], and never log card data. It replaces this class; nothing else
 * changes.
 */
@Component
class FakePaymentGateway : PaymentGateway {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun refund(
        paymentId: Long,
        amount: BigDecimal,
        idempotencyKey: String,
    ) {
        log.info("fake gateway: refunded {} on payment {} (key {})", amount, paymentId, idempotencyKey)
    }
}
