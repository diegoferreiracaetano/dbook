package com.dbook.application.identity.refreshtokenusecase

import com.dbook.domain.identity.InvalidTokenException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ASpentTokenSeenAgainRevokesTheWholeFamilyTest : RefreshTokenUseCaseFixture() {
    @Test
    fun `given a refreshed token when its old copy comes back then the new token and the whole family are revoked`() {
        val family = refreshTokens.also { useCase }.saved.first().familyId
        val child = refresh()

        assertFailsWith<InvalidTokenException> { refresh() }

        assertEquals(listOf(family), refreshTokens.familiesRevoked)
        assertTrue(refreshTokens.saved.all { it.revoked })
        assertFailsWith<InvalidTokenException> { useCase.execute(child.refreshToken) }
        // the old copy, and then the child, which the revocation had already spent: both are reuses
        assertEquals(2.0, meters.counter("dbook.auth.refresh", "outcome", "reuse_detected").count())
    }

    @Test
    fun `given a refresh when it succeeds then the new token joins the family of the old one`() {
        val old = refreshTokens.also { useCase }.saved.first()

        refresh()

        assertEquals(setOf(old.familyId), refreshTokens.saved.map { it.familyId }.toSet())
    }

    @Test
    fun `given an unknown or expired token when refreshing then nothing is revoked`() {
        assertFailsWith<InvalidTokenException> { refresh("never-issued") }

        assertTrue(refreshTokens.familiesRevoked.isEmpty())
    }
}
