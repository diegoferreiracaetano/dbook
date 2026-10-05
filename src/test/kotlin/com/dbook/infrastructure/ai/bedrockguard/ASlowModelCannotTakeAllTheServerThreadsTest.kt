package com.dbook.infrastructure.ai.bedrockguard

import com.dbook.domain.ai.AiServiceUnavailableException
import com.dbook.infrastructure.ai.BedrockGuardSettings
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ASlowModelCannotTakeAllTheServerThreadsTest : BedrockGuardFixture() {
    @Test
    fun `given the allowed calls are in flight when one more comes then it is refused at once`() {
        val guard = guardOf(BedrockGuardSettings(maxConcurrentCalls = 2))
        val inside = CountDownLatch(2)
        val release = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(2)
        try {
            repeat(2) {
                pool.submit {
                    guard.call {
                        inside.countDown()
                        release.await()
                    }
                }
            }
            assertTrue(inside.await(5, TimeUnit.SECONDS))

            assertFailsWith<AiServiceUnavailableException> { guard.call { "never runs" } }

            assertEquals(1.0, count("bulkhead_full"))
        } finally {
            release.countDown()
            pool.shutdown()
        }
    }
}
