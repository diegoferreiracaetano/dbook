package com.dbook.application.identity.acceptinvitationusecase

import com.dbook.application.identity.AcceptInvitationCommand
import com.dbook.application.identity.staff.StaffUseCaseFixture
import com.dbook.domain.identity.InvalidInvitationException
import kotlin.test.Test
import kotlin.test.assertFailsWith

class ALinkWorksOnlyOnceTest : StaffUseCaseFixture() {
    @Test
    fun `given a link already used when accepting again then it is the uniform invalid invitation error`() {
        seedInvitation(token = "abc")
        accept.execute(AcceptInvitationCommand("abc", "Maria", "a-long-passphrase-1"))

        assertFailsWith<InvalidInvitationException> {
            accept.execute(AcceptInvitationCommand("abc", "Other", "another-long-passphrase-2"))
        }
    }
}
