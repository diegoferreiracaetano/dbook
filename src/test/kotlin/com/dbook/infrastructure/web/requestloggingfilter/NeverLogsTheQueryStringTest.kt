package com.dbook.infrastructure.web.requestloggingfilter

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class NeverLogsTheQueryStringTest : RequestLoggingFixture() {
    @Test
    fun `given a request with a secret in the query string when it ends then the access line does not contain it`() {
        getFromApi("/health?token=super-secret-value")

        val line = accessLinesFor("/health").single().formattedMessage

        assertEquals(true, line.startsWith("GET /health -> "), line)
        assertFalse(line.contains("super-secret-value"), line)
        assertFalse(line.contains("token"), line)
    }
}
