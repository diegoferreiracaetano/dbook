package com.dbook.infrastructure.ai.bedrockguard

import com.dbook.infrastructure.ai.BedrockConfig
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals

class TheModelCallHasATimeoutTest {
    @Test
    fun `given the configured timeouts when the client is built then the whole call and each attempt are bounded`() {
        val client = BedrockConfig().bedrockRuntimeClient("us-east-1", timeoutSeconds = 7, attemptTimeoutSeconds = 3)

        val configuration = client.serviceClientConfiguration().overrideConfiguration()

        assertEquals(Duration.ofSeconds(7), configuration.apiCallTimeout().get())
        assertEquals(Duration.ofSeconds(3), configuration.apiCallAttemptTimeout().get())
    }
}
