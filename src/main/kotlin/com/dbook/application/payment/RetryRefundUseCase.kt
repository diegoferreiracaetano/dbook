package com.dbook.application.payment

import com.dbook.domain.identity.Actor
import com.dbook.domain.payment.Refund
import io.micrometer.observation.annotation.Observed
import org.springframework.stereotype.Service

// Tries a FAILED refund again (or one a crash left REQUESTED). Safe to repeat: the gateway key is the refund's id.
@Observed(name = "dbook.usecase")
@Service
class RetryRefundUseCase(
    private val refundRegistrar: RefundRegistrar,
    private val refundProcessor: RefundProcessor,
) {
    fun execute(
        refundId: Long,
        actor: Actor,
    ): Refund = refundProcessor.process(refundRegistrar.reopen(refundId, actor), actor)
}
