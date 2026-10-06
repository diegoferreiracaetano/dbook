package com.dbook.presentation.staffmanagement

import com.dbook.domain.common.access.Role
import kotlin.test.Test
import kotlin.test.assertEquals

class ChangingARoleEndsTheSessionsAndIsAuditedTest : StaffManagementFixture() {
    @Test
    fun `given a staff member with a session when their role changes then it ends and the trail has the change`() {
        val adminToken = newSuperAdminToken()
        val email = uniqueEmail()
        onboard(adminToken, email, Role.SUPPORT)
        val id = userIdOf(email)
        val session = refreshCookieOf(adminLogin(email, "a-long-passphrase-1"))

        changeRoleOf(adminToken, id, Role.CATALOG_MANAGER).andExpect {
            status { isOk() }
            jsonPath("$.role") { value("CATALOG_MANAGER") }
        }

        adminRefresh(session).andExpect { status { isUnauthorized() } }
        val entries = auditEntries(adminToken, "action=STAFF_ROLE_CHANGED&targetId=$id")
        assertEquals(1, entries.size())
        assertEquals("SUPPORT", entries[0]["before"]["role"].asText())
        assertEquals("CATALOG_MANAGER", entries[0]["after"]["role"].asText())
    }
}
