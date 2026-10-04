package com.dbook.infrastructure.email

import org.springframework.mock.env.MockEnvironment
import kotlin.test.Test
import kotlin.test.assertFailsWith

// The invitation body carries a live token: the flag that prints it must never be on where logs are shared.
class LoggingEmailSenderRefusesToLogTheBodyInProductionTest {
    @Test
    fun `given the json profile when the body logging is on then it refuses to start, and it is fine when off`() {
        val production = MockEnvironment().apply { setActiveProfiles("json") }

        assertFailsWith<IllegalStateException> { LoggingEmailSender(logBody = true, environment = production) }
        LoggingEmailSender(logBody = false, environment = production)
        LoggingEmailSender(logBody = true, environment = MockEnvironment())
    }
}
