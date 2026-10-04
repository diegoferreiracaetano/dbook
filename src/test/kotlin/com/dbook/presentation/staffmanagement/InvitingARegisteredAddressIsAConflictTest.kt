package com.dbook.presentation.staffmanagement

import kotlin.test.Test

class InvitingARegisteredAddressIsAConflictTest : StaffManagementFixture() {
    @Test
    fun `given an address that already has an account when inviting in another case then it is 409 and no mail`() {
        val adminToken = newSuperAdminToken()
        val email = uniqueEmail()
        registerAndLogin(email)
        val mailsBefore = emails.sent.size

        invite(adminToken, email.uppercase()).andExpect {
            status { isConflict() }
            jsonPath("$.code") { value("CONFLICT") }
        }

        kotlin.test.assertEquals(mailsBefore, emails.sent.size)
    }
}
