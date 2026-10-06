package com.dbook.presentation.securityintegration

import com.dbook.domain.common.access.Role
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.post
import kotlin.test.Test

class SupportCannotWriteFlightsTest : SecurityIntegrationFixture() {
    @Test
    fun `given a SUPPORT token when posting to admin flights then it returns 403 for lack of FLIGHT_WRITE`() {
        val token = registerStaffAndLogin(uniqueEmail(), Role.SUPPORT)

        // a well-formed body on purpose: the permission check runs after the body is bound
        mockMvc.post("/v1/admin/flights") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = validFlightRequestBody()
        }.andExpect { status { isForbidden() } }
    }
}
