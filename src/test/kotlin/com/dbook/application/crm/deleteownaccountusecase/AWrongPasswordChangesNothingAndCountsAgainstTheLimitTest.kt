package com.dbook.application.crm.deleteownaccountusecase

import com.dbook.application.crm.CrmUseCaseFixture
import com.dbook.application.crm.DeleteOwnAccountCommand
import com.dbook.domain.identity.InvalidCredentialsException
import com.dbook.domain.identity.TooManyLoginAttemptsException
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class AWrongPasswordChangesNothingAndCountsAgainstTheLimitTest : CrmUseCaseFixture() {
    @Test
    fun `given wrong passwords when deleting then it is refused, nothing changes and the third try locks it`() {
        repeat(3) {
            assertFailsWith<InvalidCredentialsException> {
                deleteOwnAccount.execute(DeleteOwnAccountCommand(customerId, "wrong", "10.0.0.1"))
            }
        }

        assertFailsWith<TooManyLoginAttemptsException> {
            deleteOwnAccount.execute(DeleteOwnAccountCommand(customerId, "current-password-1", "10.0.0.1"))
        }
        assertFalse(users.findById(customerId)?.isAnonymized == true)
        assertFalse(audit.events.any())
    }
}
