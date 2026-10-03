package com.dbook.infrastructure.observability

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReadinessIsUpWhenPostgresAndRedisAreReachableTest : ObservabilityFixture() {
    @Test
    fun `given reachable dependencies when asking for readiness then it is UP`() {
        val response = getFromManagement("/actuator/health/readiness")

        assertEquals(200, response.statusCode())
        assertTrue(response.body().contains("\"status\":\"UP\""), response.body())
    }
}
