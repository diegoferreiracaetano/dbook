package com.dbook.application.identity.acceptinvitationusecase

import com.dbook.application.identity.AcceptInvitationCommand
import com.dbook.application.identity.staff.StaffUseCaseFixture
import com.dbook.domain.identity.InvalidInvitationException
import kotlin.test.Test
import kotlin.test.assertFailsWith

class RefusesAnUnknownTokenTest : StaffUseCaseFixture() {
    @Test
    fun `given a token nobody was given when accepting then it is the uniform invalid invitation error`() {
        assertFailsWith<InvalidInvitationException> {
            accept.execute(AcceptInvitationCommand("nope", "Maria", "a-long-passphrase-1"))
        }
    }
}
