package com.dbook.infrastructure.observability

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PrometheusExposesJvmAndHttpMetricsTest : ObservabilityFixture() {
    @Test
    fun `given a served request when scraping prometheus then jvm and http metrics are there`() {
        // one request first, so there is an http_server_requests series to report
        getFromApi("/health")

        val response = getFromManagement("/actuator/prometheus")

        assertEquals(200, response.statusCode())
        assertTrue(response.body().contains("jvm_memory_used_bytes"), "no JVM metrics")
        assertTrue(response.body().contains("http_server_requests_seconds_count"), "no HTTP metrics")
    }
}
