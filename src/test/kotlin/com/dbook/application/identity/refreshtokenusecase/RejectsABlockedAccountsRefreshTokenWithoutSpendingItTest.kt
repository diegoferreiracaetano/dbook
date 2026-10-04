package com.dbook.application.identity.refreshtokenusecase

import com.dbook.domain.identity.AccountBlockedException
import com.dbook.domain.identity.Role
import com.dbook.domain.identity.User
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class RejectsABlockedAccountsRefreshTokenWithoutSpendingItTest : RefreshTokenUseCaseFixture() {
    override val existingUser =
        User(id = 1, email = "diego@example.com", passwordHash = "x", name = "Diego", role = Role.CLIENT)
            .block("chargeback fraud", now)

    @Test
    fun `given a blocked account when refreshing then it throws AccountBlockedException and the token is untouched`() {
        assertFailsWith<AccountBlockedException> { refresh() }

        assertFalse(tokenIsSpent())
    }
}
