package com.dbook.application.payment

import com.dbook.application.common.countOutcome
import com.dbook.domain.payment.Refund
import com.dbook.domain.payment.RefundAlreadyRequestedException
import com.dbook.domain.payment.RefundRepository
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.observation.annotation.Observed
import org.springframework.stereotype.Service

/**
 * Refunds a CONFIRMED booking in full. Not transactional as a whole, on purpose: it is a saga of three steps
 * ([RefundRegistrar], the gateway call in [RefundProcessor], then [RefundSettler]), each with its own transaction.
 * The result is the refund as it ended, COMPLETED or FAILED (money did not move; it can be retried).
 */
@Observed(name = "dbook.usecase")
@Service
class RefundBookingUseCase(
    private val refundRepository: RefundRepository,
    private val refundRegistrar: RefundRegistrar,
    private val refundProcessor: RefundProcessor,
    private val meterRegistry: MeterRegistry,
) {
    fun execute(command: RefundCommand): Refund {
        val fingerprint = command.fingerprint()
        return idempotently(
            previous = findByKey(command),
            previousFingerprint = { it.requestFingerprint },
            fingerprint = fingerprint,
            onReplay = { meterRegistry.countOutcome("dbook.refund", "replayed") },
        ) { registerAndProcess(command, fingerprint) }
    }

    private fun registerAndProcess(
        command: RefundCommand,
        fingerprint: String,
    ): Refund {
        val registered =
            try {
                refundRegistrar.register(command)
            } catch (ex: RefundAlreadyRequestedException) {
                // the same request twice at once: the other one won the insert, and is the refund to answer with
                val raced = findByKey(command)
                if (raced != null && raced.requestFingerprint == fingerprint) {
                    meterRegistry.countOutcome("dbook.refund", "replayed")
                    return raced
                }
                meterRegistry.countOutcome("dbook.refund", "conflict")
                throw ex
            }
        return refundProcessor.process(registered, command.actor)
    }

    private fun findByKey(command: RefundCommand) =
        refundRepository.findByRequestedByAndIdempotencyKey(command.actor.id, command.idempotencyKey)
}
