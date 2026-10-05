package com.dbook.infrastructure.ai.bedrockguard

import com.dbook.infrastructure.ai.BedrockGuardSettings
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import software.amazon.awssdk.core.exception.SdkException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TheCircuitClosesAgainWhenTheModelRecoversTest : BedrockGuardFixture() {
    @Test
    fun `given an open circuit when the wait is over and the trial calls work then it closes`() {
        val guard = guardOf(BedrockGuardSettings(minimumCalls = 2, windowSize = 2, openSeconds = 1, trialCalls = 2))
        repeat(2) { assertFailsWith<SdkException> { guard.call { serviceDown() } } }
        assertEquals(CircuitBreaker.State.OPEN, guard.state)

        Thread.sleep(OPEN_WAIT_MS)
        repeat(2) { guard.call { "fine" } }

        assertEquals(CircuitBreaker.State.CLOSED, guard.state)
    }

    private companion object {
        const val OPEN_WAIT_MS = 1_200L
    }
}
