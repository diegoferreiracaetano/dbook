package com.dbook

import com.dbook.domain.payment.PaymentGateway
import com.dbook.domain.payment.PaymentGatewayException
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import java.math.BigDecimal
import java.util.concurrent.CopyOnWriteArrayList

// Replaces the always-succeeding gateway in every integration test (one shared context), so a test can make the
// processor refuse and see what the refund saga does. A test that sets [failure] clears it in a `finally`.
class ControllablePaymentGateway : PaymentGateway {
    data class Call(val paymentId: Long, val amount: BigDecimal, val idempotencyKey: String)

    val calls = CopyOnWriteArrayList<Call>()

    @Volatile
    var failure: String? = null

    override fun refund(
        paymentId: Long,
        amount: BigDecimal,
        idempotencyKey: String,
    ) {
        calls += Call(paymentId, amount, idempotencyKey)
        failure?.let { throw PaymentGatewayException(it) }
    }
}

@TestConfiguration
class PaymentGatewayTestConfig {
    @Bean
    @Primary
    fun controllablePaymentGateway() = ControllablePaymentGateway()
}
