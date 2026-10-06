package com.dbook.presentation.securityintegration

import com.dbook.domain.common.access.Role
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.post
import kotlin.test.Test
import kotlin.test.assertEquals

class ADeniedAdminCallIsAuditedTest : SecurityIntegrationFixture() {
    @Test
    fun `given a support agent denied on admin flights when the trail is read then the denial is recorded`() {
        val supportEmail = uniqueEmail()
        val supportToken = registerStaffAndLogin(supportEmail, Role.SUPPORT)
        val adminToken = registerStaffAndLogin(uniqueEmail())
        mockMvc.post("/v1/admin/flights") {
            header("Authorization", "Bearer $supportToken")
            contentType = MediaType.APPLICATION_JSON
            content = validFlightRequestBody()
        }.andExpect { status { isForbidden() } }

        val entries = auditEntries(adminToken, "actorId=${userIdOf(supportEmail)}&action=ACCESS_DENIED")

        assertEquals(1, entries.size())
        assertEquals("DENIED", entries[0]["outcome"].asText())
        assertEquals("SUPPORT", entries[0]["actorRole"].asText())
        assertEquals("ENDPOINT", entries[0]["targetType"].asText())
        assertEquals("POST /v1/admin/flights", entries[0]["targetId"].asText())
    }
}
