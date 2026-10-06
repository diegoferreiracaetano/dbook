package com.dbook.application.identity.registeruserusecase

import com.dbook.application.identity.RegisterUserCommand
import com.dbook.domain.identity.UserAlreadyExistsException
import kotlin.test.Test
import kotlin.test.assertFailsWith

class RejectsTheAddressOfAnAnonymizedAccountTest : RegisterUserUseCaseFixture() {
    @Test
    fun `given the address of an anonymized account when registering then it is refused, in any case`() {
        anonymizedEmails.remember("maria@example.com")

        assertFailsWith<UserAlreadyExistsException> {
            register(RegisterUserCommand("Maria@Example.com", "s3cret-password", "Maria"))
        }
    }
}
