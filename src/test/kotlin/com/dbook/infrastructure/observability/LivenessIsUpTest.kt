package com.dbook.infrastructure.observability

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LivenessIsUpTest : ObservabilityFixture() {
    @Test
    fun `given a running application when asking for liveness then it is UP`() {
        val response = getFromManagement("/actuator/health/liveness")

        assertEquals(200, response.statusCode())
        assertTrue(response.body().contains("\"status\":\"UP\""), response.body())
    }
}
