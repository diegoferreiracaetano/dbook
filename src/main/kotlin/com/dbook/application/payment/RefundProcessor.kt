package com.dbook.application.payment

import com.dbook.domain.identity.Actor
import com.dbook.domain.payment.PaymentGateway
import com.dbook.domain.payment.PaymentGatewayException
import com.dbook.domain.payment.Refund
import org.springframework.stereotype.Service

// The middle of the saga, and deliberately not transactional: the gateway is a network call, and holding a database
// transaction (and its locks) open while waiting for it would turn a slow processor into a stuck booking table.
// The gateway gets the refund's own id as its idempotency key, the same on every attempt, so asking twice for the
// same refund can never move the money twice.
@Service
class RefundProcessor(
    private val paymentGateway: PaymentGateway,
    private val refundSettler: RefundSettler,
) {
    fun process(
        refund: Refund,
        actor: Actor,
    ): Refund {
        val refundId = requireNotNull(refund.id)
        try {
            paymentGateway.refund(refund.paymentId, refund.amount, "refund-$refundId")
        } catch (ex: PaymentGatewayException) {
            return refundSettler.fail(refundId, actor, ex.message ?: "the payment gateway refused the refund")
        }
        return refundSettler.complete(refundId, actor)
    }
}
