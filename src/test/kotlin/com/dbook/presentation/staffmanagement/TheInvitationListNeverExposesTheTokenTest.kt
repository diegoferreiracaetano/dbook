package com.dbook.presentation.staffmanagement

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TheInvitationListNeverExposesTheTokenTest : StaffManagementFixture() {
    @Test
    fun `given invitations when listing then the token and its hash are nowhere in the response`() {
        val adminToken = newSuperAdminToken()
        val email = uniqueEmail()
        invite(adminToken, email).andExpect { status { isCreated() } }
        val token = tokenMailedTo(email)

        val body = listInvitations(adminToken).response.contentAsString

        assertTrue(body.contains(email))
        assertFalse(body.contains(token))
        assertFalse(body.lowercase().contains("token"))
    }
}
