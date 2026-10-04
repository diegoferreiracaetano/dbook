package com.dbook.application.identity.bootstrapsuperadminusecase

import com.dbook.domain.identity.UserAlreadyExistsException
import kotlin.test.Test
import kotlin.test.assertFailsWith

class FailsWhenTheAddressIsAlreadyAnAccountTest : BootstrapSuperAdminUseCaseFixture() {
    @Test
    fun `given the address of a customer when bootstrapping then it fails instead of promoting them`() {
        assertFailsWith<UserAlreadyExistsException> { bootstrap(email = "customer@example.com") }
    }
}
