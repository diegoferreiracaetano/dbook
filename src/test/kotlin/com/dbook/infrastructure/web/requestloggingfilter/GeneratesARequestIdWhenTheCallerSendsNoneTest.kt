package com.dbook.infrastructure.web.requestloggingfilter

import kotlin.test.Test
import kotlin.test.assertTrue

class GeneratesARequestIdWhenTheCallerSendsNoneTest : RequestLoggingFixture() {
    @Test
    fun `given a request without X-Request-Id when it is answered then the response carries a generated one`() {
        val requestId = getFromApi("/health").headers().firstValue("X-Request-Id").orElse("")

        assertTrue(Regex("^[0-9a-f-]{36}$").matches(requestId), "not a generated UUID: '$requestId'")
    }
}
