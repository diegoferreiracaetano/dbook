package com.dbook.infrastructure.web.requestloggingfilter

import kotlin.test.Test
import kotlin.test.assertTrue

class LogsTheTraceIdOfTheRequestTest : RequestLoggingFixture() {
    @Test
    fun `given a request when its access line is written then it carries the id of the request's trace`() {
        getFromApi("/health")

        val traceId = accessLinesFor("/health").single().mdcPropertyMap["traceId"]

        assertTrue(Regex("^[0-9a-f]{32}$").matches(traceId.orEmpty()), "no trace id in the MDC: '$traceId'")
    }
}
