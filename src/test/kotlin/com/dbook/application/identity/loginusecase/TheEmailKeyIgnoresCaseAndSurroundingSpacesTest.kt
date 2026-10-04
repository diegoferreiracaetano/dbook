package com.dbook.application.identity.loginusecase

import com.dbook.domain.identity.InvalidCredentialsException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TheEmailKeyIgnoresCaseAndSurroundingSpacesTest : LoginUseCaseFixture() {
    @Test
    fun `given failures typed with different case when counting then they all land on the same email`() {
        assertFailsWith<InvalidCredentialsException> { login(email = "DIEGO@example.com", password = "x") }
        assertFailsWith<InvalidCredentialsException> { login(email = " diego@example.com ", password = "x") }

        assertEquals(2, limiter.failuresOf("email:diego@example.com"))
    }
}
