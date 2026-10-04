package com.dbook.infrastructure.persistence.identity

import org.springframework.dao.OptimisticLockingFailureException
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertFailsWith

// Two accepts of the same link read the same version; the second one must lose instead of creating a second user.
class AStaleInvitationCannotBeAcceptedTwiceTest : StaffInvitationRepositoryAdapterFixture() {
    @Test
    fun `given two copies of one invitation when both are accepted then the second save fails on the version`() {
        val saved = invitations.save(issue(uniqueEmail(), inviterId()))
        val first = requireNotNull(invitations.findById(requireNotNull(saved.id)))
        val second = requireNotNull(invitations.findById(requireNotNull(saved.id)))

        invitations.save(first.accept(Instant.now()))

        assertFailsWith<OptimisticLockingFailureException> { invitations.save(second.accept(Instant.now())) }
    }
}
