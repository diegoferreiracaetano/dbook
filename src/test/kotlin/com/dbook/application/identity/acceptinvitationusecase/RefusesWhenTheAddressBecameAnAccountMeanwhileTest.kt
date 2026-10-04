package com.dbook.application.identity.acceptinvitationusecase

import com.dbook.application.identity.AcceptInvitationCommand
import com.dbook.application.identity.staff.StaffUseCaseFixture
import com.dbook.domain.identity.UserAlreadyExistsException
import kotlin.test.Test
import kotlin.test.assertFailsWith

class RefusesWhenTheAddressBecameAnAccountMeanwhileTest : StaffUseCaseFixture() {
    @Test
    fun `given the invited address registered meanwhile when accepting then no second account is created`() {
        seedInvitation(email = "customer@example.com", token = "abc")

        assertFailsWith<UserAlreadyExistsException> {
            accept.execute(AcceptInvitationCommand("abc", "Maria", "a-long-passphrase-1"))
        }
    }
}
