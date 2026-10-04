package com.dbook.application.identity.refreshtokenusecase

import com.dbook.application.identity.SessionAudience
import com.dbook.domain.identity.InvalidTokenException
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class RejectsAClientsTokenOnTheStaffAudienceTest : RefreshTokenUseCaseFixture() {
    @Test
    fun `given a CLIENT refresh token when refreshing through the portal then it is invalid and not spent`() {
        assertFailsWith<InvalidTokenException> { refresh(audience = SessionAudience.STAFF) }

        assertFalse(tokenIsSpent())
    }
}
