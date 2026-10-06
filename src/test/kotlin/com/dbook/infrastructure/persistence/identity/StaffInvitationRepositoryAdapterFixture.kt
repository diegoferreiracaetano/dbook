package com.dbook.infrastructure.persistence.identity

import com.dbook.AbstractIntegrationTest
import com.dbook.domain.common.access.Role
import com.dbook.domain.identity.StaffInvitation
import com.dbook.domain.identity.StaffInvitationRepository
import com.dbook.domain.identity.User
import com.dbook.domain.identity.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant

abstract class StaffInvitationRepositoryAdapterFixture : AbstractIntegrationTest() {
    @Autowired
    lateinit var invitations: StaffInvitationRepository

    @Autowired
    lateinit var users: UserRepository

    protected fun uniqueEmail() = "invite${(1..999_999_999).random()}@example.com"

    // invited_by is a real foreign key, so there has to be a real inviter
    protected fun inviterId(): Long =
        requireNotNull(users.save(User(email = uniqueEmail(), passwordHash = "h", name = "Inviter")).id)

    protected fun issue(
        email: String,
        invitedBy: Long,
        tokenHash: String = "hash-${(1..999_999_999).random()}",
    ) = StaffInvitation.issue(email, Role.SUPPORT, tokenHash, invitedBy, Instant.now())
}
