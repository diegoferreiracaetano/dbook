package com.dbook.infrastructure.persistence.identity

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

// The reason recordLogin is a targeted UPDATE: saving a user read before the block would put ACTIVE back.
class RecordingALoginDoesNotUndoABlockTest : UserRepositoryAdapterFixture() {
    @Test
    fun `given a user blocked after it was read when the login is recorded then it stays blocked`() {
        val id = requireNotNull(newUser().id)
        userRepository.save(requireNotNull(userRepository.findById(id)).block("spam", Instant.now()))
        val loggedInAt = Instant.parse("2026-10-04T12:00:00Z")

        userRepository.recordLogin(id, loggedInAt)

        val reloaded = requireNotNull(userRepository.findById(id))
        assertTrue(reloaded.isBlocked)
        assertEquals(loggedInAt, reloaded.lastLoginAt)
        assertNotNull(reloaded.blockedAt)
    }
}
