package com.dbook.application.identity.loginusecase

import com.dbook.application.identity.SessionAudience
import com.dbook.domain.identity.InvalidCredentialsException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class RejectsAClientOnTheStaffAudienceTest : LoginUseCaseFixture() {
    @Test
    fun `given a CLIENT with the right password when logging into the staff portal then it is a wrong password`() {
        assertFailsWith<InvalidCredentialsException> { login(audience = SessionAudience.STAFF) }

        assertTrue(refreshTokenRepository.saved.isEmpty())
        assertEquals(1, limiter.failuresOf("email:diego@example.com"))
        assertEquals(1.0, count("invalid_credentials", audience = "staff"))
    }
}
