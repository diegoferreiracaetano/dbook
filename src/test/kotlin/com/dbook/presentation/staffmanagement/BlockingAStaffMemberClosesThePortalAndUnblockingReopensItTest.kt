package com.dbook.presentation.staffmanagement

import kotlin.test.Test
import kotlin.test.assertEquals

class BlockingAStaffMemberClosesThePortalAndUnblockingReopensItTest : StaffManagementFixture() {
    @Test
    fun `given a staff member when blocked then the portal login is 403 ACCOUNT_BLOCKED, and unblocked it works`() {
        val adminToken = newSuperAdminToken()
        val email = uniqueEmail()
        onboard(adminToken, email)
        val id = userIdOf(email)

        blockStaff(adminToken, id).andExpect {
            status { isOk() }
            jsonPath("$.status") { value("BLOCKED") }
        }
        val blocked = adminLogin(email, "a-long-passphrase-1")
        assertEquals(403, blocked.response.status)
        assertEquals("ACCOUNT_BLOCKED", errorCodeOf(blocked))

        unblockStaff(adminToken, id).andExpect { status { isOk() } }
        assertEquals(200, adminLogin(email, "a-long-passphrase-1").response.status)
    }
}
