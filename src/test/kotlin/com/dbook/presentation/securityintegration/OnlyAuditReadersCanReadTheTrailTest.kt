package com.dbook.presentation.securityintegration

import com.dbook.domain.identity.Role
import org.springframework.test.web.servlet.get
import kotlin.test.Test

class OnlyAuditReadersCanReadTheTrailTest : SecurityIntegrationFixture() {
    @Test
    fun `given a support agent when reading the audit trail then it is 403, for lack of AUDIT_READ`() {
        val token = registerStaffAndLogin(uniqueEmail(), Role.SUPPORT)

        mockMvc.get("/v1/admin/audit") { header("Authorization", "Bearer $token") }
            .andExpect { status { isForbidden() } }
    }
}
