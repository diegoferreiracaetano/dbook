package com.dbook.presentation.securityintegration

import org.springframework.test.web.servlet.get
import kotlin.test.Test

// No flight is registered for LIS in this scenario, so a gated endpoint would still need to
// reject with 401 before ever reaching that lookup — 404 here proves the opposite: the request
// got past security and only then found nothing to return. LIS (not GIG) on purpose: the
// Testcontainers database is shared by every integration test, and the others register GIG
// flights, which would turn this 404 into a 200 depending on execution order.
class LowestPriceRemainsPublicWithoutATokenTest : SecurityIntegrationFixture() {
    @Test
    fun `given no token when getting the lowest price then it is not blocked by auth`() {
        mockMvc.get("/v1/flights/lowest-price") {
            param("destination", "LIS")
        }.andExpect { status { isNotFound() } }
    }
}
