package com.dbook.presentation.staffmanagement

import kotlin.test.Test

class ReinvitingKillsTheOldLinkTest : StaffManagementFixture() {
    @Test
    fun `given a second invitation to the same address when the old link is used then it is refused`() {
        val adminToken = newSuperAdminToken()
        val email = uniqueEmail()
        invite(adminToken, email).andExpect { status { isCreated() } }
        val oldToken = tokenMailedTo(email)
        invite(adminToken, email).andExpect { status { isCreated() } }
        val newToken = tokenMailedTo(email)

        accept(oldToken).andExpect { status { isBadRequest() } }
        accept(newToken).andExpect { status { isCreated() } }
    }
}
