package com.dbook.infrastructure.persistence.identity

import org.springframework.dao.OptimisticLockingFailureException
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

// The case the version column exists for: someone renames a user from a copy read before a block was applied.
class AStaleUserCopyCannotOverwriteABlockTest : UserRepositoryAdapterFixture() {
    @Test
    fun `given a user read before a block when the stale copy is saved then it fails and the block stands`() {
        val id = requireNotNull(newUser().id)
        val staleCopy = requireNotNull(userRepository.findById(id))
        userRepository.save(requireNotNull(userRepository.findById(id)).block("spam", Instant.now()))

        assertFailsWith<OptimisticLockingFailureException> { userRepository.save(staleCopy.rename("New Name")) }

        assertTrue(requireNotNull(userRepository.findById(id)).isBlocked)
    }
}
