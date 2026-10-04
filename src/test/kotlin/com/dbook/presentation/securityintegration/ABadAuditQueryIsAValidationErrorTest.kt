package com.dbook.presentation.securityintegration

import org.springframework.test.web.servlet.get
import kotlin.test.Test
import kotlin.test.assertEquals

class ABadAuditQueryIsAValidationErrorTest : SecurityIntegrationFixture() {
    @Test
    fun `given an unknown action, a broken cursor or a too large page when reading the trail then each is 400`() {
        val token = registerStaffAndLogin(uniqueEmail())

        listOf("action=NOT_AN_ACTION", "cursor=not-a-cursor", "size=101", "size=0", "from=yesterday").forEach { query ->
            val result =
                mockMvc.get("/v1/admin/audit?$query") { header("Authorization", "Bearer $token") }.andReturn()

            assertEquals(400, result.response.status, query)
            assertEquals("VALIDATION_FAILED", errorCodeOf(result), query)
        }
    }
}
