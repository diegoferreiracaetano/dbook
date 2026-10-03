package com.dbook.infrastructure.web.requestloggingfilter

import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ReplacesAnUnsafeRequestIdTest : RequestLoggingFixture() {
    @Test
    fun `given an X-Request-Id with spaces and symbols when answered then it is replaced by a generated one`() {
        val unsafe = "not safe: {\"a\":1}"

        val echoed =
            getFromApi(
                "/health",
                mapOf("X-Request-Id" to unsafe),
            ).headers().firstValue("X-Request-Id").orElse("")

        assertNotEquals(unsafe, echoed)
        assertTrue(Regex("^[0-9a-f-]{36}$").matches(echoed), "expected a generated UUID, got '$echoed'")
    }
}
