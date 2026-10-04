package com.dbook.presentation.staffmanagement

import kotlin.test.Test
import kotlin.test.assertEquals

class ARevokedInvitationCannotBeAcceptedTest : StaffManagementFixture() {
    @Test
    fun `given a revoked invitation when its link is used then it is refused and the list shows REVOKED`() {
        val adminToken = newSuperAdminToken()
        val email = uniqueEmail()
        val id = idOf(invite(adminToken, email))

        revokeInvitation(adminToken, id).andExpect { status { isNoContent() } }

        accept(tokenMailedTo(email)).andExpect { status { isBadRequest() } }
        val listed = json(listInvitations(adminToken)).first { it["id"].asLong() == id }
        assertEquals("REVOKED", listed["status"].asText())
    }
}
