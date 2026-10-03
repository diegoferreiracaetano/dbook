package com.dbook.infrastructure.web.requestloggingfilter

import kotlin.test.Test
import kotlin.test.assertEquals

class KeepsAPlainRequestIdFromTheCallerTest : RequestLoggingFixture() {
    @Test
    fun `given a plain X-Request-Id when the request is answered then the same id is echoed back`() {
        val response = getFromApi("/health", mapOf("X-Request-Id" to "trace-me-123"))

        assertEquals("trace-me-123", response.headers().firstValue("X-Request-Id").orElse(""))
    }
}
