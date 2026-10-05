package com.dbook.infrastructure.ai

import com.dbook.domain.ai.AiServiceUnavailableException
import io.github.resilience4j.bulkhead.Bulkhead
import io.github.resilience4j.bulkhead.BulkheadConfig
import io.github.resilience4j.bulkhead.BulkheadFullException
import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import software.amazon.awssdk.core.exception.SdkException
import java.time.Duration

/** `ai.bedrock.guard.*`: when the model is considered down, and how many calls may wait on it at once. */
@ConfigurationProperties(prefix = "ai.bedrock.guard")
data class BedrockGuardSettings(
    val windowSize: Int = 10,
    val minimumCalls: Int = 5,
    val failureRatePercent: Float = 50f,
    val openSeconds: Long = 30,
    val trialCalls: Int = 2,
    val maxConcurrentCalls: Int = 10,
)

/**
 * Protects the API from a slow or broken model. Two guards in front of every call, both answering with the same
 * [AiServiceUnavailableException] (a 503) so the customer, who has the plain search anyway, is never kept waiting:
 * - a **circuit breaker**: when half of the last calls failed (a service error or a timeout), the circuit opens and
 *   calls are refused at once for `openSeconds`, then a few trial calls decide whether it closes again. Without it,
 *   every request would sit for the full timeout while the model is down, taking a server thread each;
 * - a **bulkhead**: at most `maxConcurrentCalls` calls are in flight, so a slow model can take some of the server's
 *   threads, never all of them. The excess is refused immediately, not queued.
 * Only the failures of the call itself count (`SdkException`); a bad answer from the model is not the model being down.
 * `dbook_ai_bedrock_calls_total{outcome}` and the `dbook_ai_circuit_state` gauge (0 closed, 1 open, 2 half-open)
 * show it from outside.
 */
@Component
class BedrockGuard(
    settings: BedrockGuardSettings,
    private val meterRegistry: MeterRegistry,
) {
    private val breaker: CircuitBreaker =
        CircuitBreaker.of(
            "bedrock",
            CircuitBreakerConfig.custom()
                .slidingWindowSize(settings.windowSize)
                .minimumNumberOfCalls(settings.minimumCalls)
                .failureRateThreshold(settings.failureRatePercent)
                .waitDurationInOpenState(Duration.ofSeconds(settings.openSeconds))
                .permittedNumberOfCallsInHalfOpenState(settings.trialCalls)
                .recordExceptions(SdkException::class.java)
                .build(),
        )
    private val bulkhead: Bulkhead =
        Bulkhead.of(
            "bedrock",
            BulkheadConfig.custom().maxConcurrentCalls(settings.maxConcurrentCalls).maxWaitDuration(Duration.ZERO)
                .build(),
        )

    init {
        Gauge.builder("dbook.ai.circuit.state", breaker) { stateNumber(it.state) }.register(meterRegistry)
    }

    val state: CircuitBreaker.State get() = breaker.state

    fun <T> call(block: () -> T): T =
        try {
            Bulkhead.decorateSupplier(bulkhead) { breaker.executeSupplier(block) }.get().also { count("success") }
        } catch (ex: CallNotPermittedException) {
            count("circuit_open")
            throw AiServiceUnavailableException("The AI service is paused after repeated failures, try again soon", ex)
        } catch (ex: BulkheadFullException) {
            count("bulkhead_full")
            throw AiServiceUnavailableException("The AI service is busy, try again soon", ex)
        } catch (ex: SdkException) {
            count("failure")
            throw ex
        }

    private fun count(outcome: String) {
        meterRegistry.counter("dbook.ai.bedrock.calls", "outcome", outcome).increment()
    }

    private fun stateNumber(state: CircuitBreaker.State): Double =
        when (state) {
            CircuitBreaker.State.CLOSED -> 0.0
            CircuitBreaker.State.HALF_OPEN -> 2.0
            else -> 1.0
        }
}
