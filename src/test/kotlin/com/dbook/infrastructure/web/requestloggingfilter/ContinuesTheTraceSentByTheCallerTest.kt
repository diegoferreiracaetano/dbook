package com.dbook.infrastructure.web.requestloggingfilter

import kotlin.test.Test
import kotlin.test.assertEquals

// Another service (or the mobile app, one day) calls this API inside its own trace and says so in
// the W3C `traceparent` header: the work done here must join that trace, not start a new one.
class ContinuesTheTraceSentByTheCallerTest : RequestLoggingFixture() {
    @Test
    fun `given a traceparent header when the request is served then it joins the callers trace`() {
        val callersTrace = "4bf92f3577b34da6a3ce929d0e0e4736"

        getFromApi("/health", mapOf("traceparent" to "00-$callersTrace-00f067aa0ba902b7-01"))

        assertEquals(callersTrace, accessLinesFor("/health").single().mdcPropertyMap["traceId"])
    }
}
