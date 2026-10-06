package com.dbook.presentation.securityintegration

import com.dbook.domain.common.access.Role
import org.hamcrest.Matchers.hasItem
import org.hamcrest.Matchers.not
import org.springframework.test.web.servlet.get
import kotlin.test.Test

class AdminMeReturnsTheRoleAndThePermissionsTest : SecurityIntegrationFixture() {
    @Test
    fun `given a SUPPORT agent when asking who they are then the role and exactly its permissions come back`() {
        val token = registerStaffAndLogin(uniqueEmail(), Role.SUPPORT)

        mockMvc.get("/v1/admin/auth/me") { header("Authorization", "Bearer $token") }
            .andExpect {
                status { isOk() }
                jsonPath("$.role") { value("SUPPORT") }
                jsonPath("$.permissions.length()") { value(Role.SUPPORT.permissions.size) }
                jsonPath("$.permissions") { value(hasItem("CUSTOMER_READ")) }
                jsonPath("$.permissions") { value(not(hasItem("FLIGHT_WRITE"))) }
            }
    }
}
