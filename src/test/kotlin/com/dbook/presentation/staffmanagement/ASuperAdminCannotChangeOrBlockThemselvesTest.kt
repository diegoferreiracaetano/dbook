package com.dbook.presentation.staffmanagement

import com.dbook.domain.common.access.Role
import kotlin.test.Test

class ASuperAdminCannotChangeOrBlockThemselvesTest : StaffManagementFixture() {
    @Test
    fun `given a SUPER_ADMIN when changing or blocking their own account then it is 409`() {
        val email = uniqueEmail()
        val adminToken = registerStaffAndLogin(email)
        val ownId = userIdOf(email)

        changeRoleOf(adminToken, ownId, Role.SUPPORT).andExpect { status { isConflict() } }
        blockStaff(adminToken, ownId).andExpect { status { isConflict() } }
    }
}
