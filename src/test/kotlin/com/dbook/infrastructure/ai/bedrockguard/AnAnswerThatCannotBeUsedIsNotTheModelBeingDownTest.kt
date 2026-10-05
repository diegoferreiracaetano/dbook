package com.dbook.infrastructure.ai.bedrockguard

import com.dbook.infrastructure.ai.BedrockGuardSettings
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AnAnswerThatCannotBeUsedIsNotTheModelBeingDownTest : BedrockGuardFixture() {
    @Test
    fun `given many bad answers when called again then the circuit stays closed`() {
        val guard = guardOf(BedrockGuardSettings(minimumCalls = 3))

        repeat(10) { assertFailsWith<IllegalStateException> { guard.call { error("not the format we asked") } } }

        assertEquals(CircuitBreaker.State.CLOSED, guard.state)
        assertEquals("fine", guard.call { "fine" })
    }
}
