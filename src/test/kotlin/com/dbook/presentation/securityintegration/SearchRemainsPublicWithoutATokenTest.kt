package com.dbook.presentation.securityintegration

import org.springframework.test.web.servlet.get
import kotlin.test.Test

class SearchRemainsPublicWithoutATokenTest : SecurityIntegrationFixture() {
    @Test
    fun `given no token when searching flights then it returns 200`() {
        mockMvc.get("/flights/search") {
            param("origin", "GRU")
            param("destination", "GIG")
            param("date", "2027-01-01")
        }.andExpect { status { isOk() } }
    }
}
