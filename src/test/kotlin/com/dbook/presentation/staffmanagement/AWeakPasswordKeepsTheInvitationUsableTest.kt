package com.dbook.presentation.staffmanagement

import kotlin.test.Test

class AWeakPasswordKeepsTheInvitationUsableTest : StaffManagementFixture() {
    @Test
    fun `given a weak password when accepting then it is a validation error and the same link still works`() {
        val adminToken = newSuperAdminToken()
        val email = uniqueEmail()
        invite(adminToken, email).andExpect { status { isCreated() } }
        val token = tokenMailedTo(email)

        accept(token, password = "short").andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("VALIDATION_FAILED") }
        }
        accept(token).andExpect { status { isCreated() } }
    }
}
