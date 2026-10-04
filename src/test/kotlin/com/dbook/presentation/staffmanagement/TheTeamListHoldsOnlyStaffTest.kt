package com.dbook.presentation.staffmanagement

import com.dbook.domain.identity.Role
import org.springframework.test.web.servlet.get
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TheTeamListHoldsOnlyStaffTest : StaffManagementFixture() {
    @Test
    fun `given a staff member and a customer when listing the team then only the staff member is there`() {
        val adminToken = newSuperAdminToken()
        val staffEmail = uniqueEmail()
        val customerEmail = uniqueEmail()
        onboard(adminToken, staffEmail, Role.SUPPORT)
        registerAndLogin(customerEmail)

        val emailsListed =
            json(mockMvc.get("/v1/admin/staff") { header("Authorization", "Bearer $adminToken") }.andReturn())
                .map { it["email"].asText() }

        assertTrue(staffEmail in emailsListed)
        assertFalse(customerEmail in emailsListed)
    }
}
