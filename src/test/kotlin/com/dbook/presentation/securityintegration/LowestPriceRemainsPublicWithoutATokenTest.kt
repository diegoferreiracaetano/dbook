package com.dbook.presentation.securityintegration

import org.springframework.test.web.servlet.get
import kotlin.test.Test

// No flight is registered for GIG in this scenario, so a gated endpoint would still need to
// reject with 401 before ever reaching that lookup — 404 here proves the opposite: the request
// got past security and only then found nothing to return.
class LowestPriceRemainsPublicWithoutATokenTest : SecurityIntegrationFixture() {
    @Test
    fun `given no token when getting the lowest price then it is not blocked by auth`() {
        mockMvc.get("/flights/lowest-price") {
            param("destination", "GIG")
        }.andExpect { status { isNotFound() } }
    }
}
