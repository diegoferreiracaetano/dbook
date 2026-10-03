package com.dbook.infrastructure.web.requestloggingfilter

import ch.qos.logback.classic.Level
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LogsOneAccessLineWithTheRequestIdTest : RequestLoggingFixture() {
    @Test
    fun `given a request when it ends then exactly one access line is logged carrying its request id`() {
        getFromApi("/health", mapOf("X-Request-Id" to "abc-1"))

        val lines = accessLinesFor("/health")

        assertEquals(1, lines.size)
        assertEquals(Level.INFO, lines.single().level)
        assertTrue(
            lines.single().formattedMessage.startsWith("GET /health -> 200 in "),
            lines.single().formattedMessage,
        )
        assertEquals("abc-1", lines.single().mdcPropertyMap["requestId"])
    }
}
