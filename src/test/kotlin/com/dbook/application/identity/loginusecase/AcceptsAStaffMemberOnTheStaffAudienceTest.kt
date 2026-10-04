package com.dbook.application.identity.loginusecase

import com.dbook.application.identity.SessionAudience
import com.dbook.domain.identity.Role
import com.dbook.domain.identity.User
import kotlin.test.Test
import kotlin.test.assertEquals

class AcceptsAStaffMemberOnTheStaffAudienceTest : LoginUseCaseFixture() {
    override val existingUser =
        User(id = 1, email = "diego@example.com", passwordHash = "x", name = "Diego", role = Role.SUPPORT)

    @Test
    fun `given a SUPPORT agent when logging into the staff portal then tokens are issued`() {
        val tokens = login(audience = SessionAudience.STAFF)

        assertEquals("access-1", tokens.accessToken)
        assertEquals(1.0, count("success", audience = "staff"))
    }
}
