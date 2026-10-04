package com.dbook.application.identity.acceptinvitationusecase

import com.dbook.application.identity.AcceptInvitationCommand
import com.dbook.application.identity.staff.StaffUseCaseFixture
import com.dbook.domain.identity.InvitationStatus
import com.dbook.domain.identity.Role
import kotlin.test.Test
import kotlin.test.assertEquals

class CreatesTheStaffAccountWithTheInvitedRoleTest : StaffUseCaseFixture() {
    @Test
    fun `given a valid token when accepting then the account has the invited role, name and password`() {
        val seeded = seedInvitation(email = "new@example.com", token = "abc", role = Role.CATALOG_MANAGER)

        val user = accept.execute(AcceptInvitationCommand("abc", " Maria ", "a-long-passphrase-1"))

        assertEquals(Role.CATALOG_MANAGER, user.role)
        assertEquals("Maria", user.name)
        assertEquals("new@example.com", user.email)
        assertEquals("hashed:a-long-passphrase-1", user.passwordHash)
        assertEquals(InvitationStatus.ACCEPTED, invitations.findById(requireNotNull(seeded.id))?.statusAt(now))
    }
}
