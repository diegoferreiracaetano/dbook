package com.dbook.infrastructure.observability

import kotlin.test.Test
import kotlin.test.assertEquals

// The reason the actuator has its own port: nothing operational may be reachable through the
// public one. If the separate port is ever removed, these stop being 401 and this fails.
class PublicPortDoesNotServeActuatorTest : ObservabilityFixture() {
    @Test
    fun `given the public api port when asking for actuator endpoints then they stay behind authentication`() {
        assertEquals(401, getFromApi("/actuator/prometheus").statusCode())
        assertEquals(401, getFromApi("/actuator/health").statusCode())
        assertEquals(401, getFromApi("/actuator/metrics").statusCode())
    }
}
