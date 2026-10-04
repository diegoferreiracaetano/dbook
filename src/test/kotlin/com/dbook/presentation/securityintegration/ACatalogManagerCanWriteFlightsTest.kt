package com.dbook.presentation.securityintegration

import com.dbook.domain.identity.Role
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.post
import kotlin.test.Test

class ACatalogManagerCanWriteFlightsTest : SecurityIntegrationFixture() {
    @Test
    fun `given a CATALOG_MANAGER token when posting to admin flights then it returns 201`() {
        val token = registerStaffAndLogin(uniqueEmail(), Role.CATALOG_MANAGER)

        mockMvc.post("/v1/admin/flights") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = validFlightRequestBody()
        }.andExpect { status { isCreated() } }
    }
}
