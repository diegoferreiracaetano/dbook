package com.dbook.application.identity.invitestaffusecase

import com.dbook.application.identity.InviteStaffCommand
import com.dbook.application.identity.staff.StaffUseCaseFixture
import com.dbook.domain.common.access.Role
import com.dbook.domain.identity.UserAlreadyExistsException
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class RejectsAnAddressThatAlreadyHasAnAccountTest : StaffUseCaseFixture() {
    @Test
    fun `given a registered address in another case when inviting then it is refused and nothing is mailed`() {
        assertFailsWith<UserAlreadyExistsException> {
            committed { invite.execute(InviteStaffCommand(actor, " Customer@Example.com", Role.SUPPORT)) }
        }

        assertTrue(emails.sent.isEmpty())
    }
}
