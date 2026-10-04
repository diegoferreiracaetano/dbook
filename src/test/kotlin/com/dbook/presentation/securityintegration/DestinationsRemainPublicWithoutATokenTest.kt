package com.dbook.presentation.securityintegration

import org.springframework.test.web.servlet.get
import kotlin.test.Test

class DestinationsRemainPublicWithoutATokenTest : SecurityIntegrationFixture() {
    @Test
    fun `given no token when listing destinations then it returns 200`() {
        mockMvc.get("/v1/destinations").andExpect { status { isOk() } }
    }
}
