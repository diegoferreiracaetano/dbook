package com.dbook.infrastructure.ai.bedrockguard

import com.dbook.domain.ai.AiServiceUnavailableException
import com.dbook.infrastructure.ai.BedrockGuardSettings
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import software.amazon.awssdk.core.exception.SdkException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AModelThatKeepsFailingIsNotCalledAgainForAWhileTest : BedrockGuardFixture() {
    @Test
    fun `given the model fails repeatedly when called again then it is refused without calling the model`() {
        val guard = guardOf(BedrockGuardSettings(minimumCalls = 5))
        repeat(5) { assertFailsWith<SdkException> { guard.call { serviceDown() } } }
        var reached = false

        assertFailsWith<AiServiceUnavailableException> { guard.call { reached = true } }

        assertEquals(CircuitBreaker.State.OPEN, guard.state)
        assertEquals(false, reached)
        assertEquals(5.0, count("failure"))
        assertEquals(1.0, count("circuit_open"))
    }

    @Test
    fun `given a few failures only when called again then the model is still tried`() {
        val guard = guardOf(BedrockGuardSettings(minimumCalls = 5))
        repeat(2) { assertFailsWith<SdkException> { guard.call { serviceDown() } } }

        assertEquals("fine", guard.call { "fine" })
        assertEquals(CircuitBreaker.State.CLOSED, guard.state)
    }
}
