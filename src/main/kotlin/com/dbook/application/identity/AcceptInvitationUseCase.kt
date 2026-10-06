package com.dbook.application.identity

import com.dbook.domain.audit.AuditAction
import com.dbook.domain.audit.AuditEvent
import com.dbook.domain.audit.AuditLog
import com.dbook.domain.identity.Actor
import com.dbook.domain.identity.InvalidInvitationException
import com.dbook.domain.identity.InvitationStatus
import com.dbook.domain.identity.PasswordHasher
import com.dbook.domain.identity.PasswordPolicy
import com.dbook.domain.identity.StaffInvitation
import com.dbook.domain.identity.StaffInvitationRepository
import com.dbook.domain.identity.TokenService
import com.dbook.domain.identity.User
import com.dbook.domain.identity.UserAlreadyExistsException
import com.dbook.domain.identity.UserRepository
import com.dbook.domain.identity.toAuditSnapshot
import io.micrometer.observation.annotation.Observed
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant

data class AcceptInvitationCommand(
    val token: String,
    val name: String,
    val password: String,
)

/** Turns a valid invitation into a staff account, with the name and the password the invitee chose. */
@Observed(name = "dbook.usecase")
@Service
class AcceptInvitationUseCase(
    private val invitations: StaffInvitationRepository,
    private val userRepository: UserRepository,
    private val tokenService: TokenService,
    private val passwordHasher: PasswordHasher,
    private val auditLog: AuditLog,
    private val clock: Clock,
) {
    @Transactional
    fun execute(command: AcceptInvitationCommand): User {
        val now = clock.instant()
        val invitation = pendingInvitation(command.token, now)
        PasswordPolicy.validate(command.password, invitation.email, PasswordPolicy.STAFF_MIN_LENGTH)

        // The invitation is closed first: two accepts of the same link read the same version, so the second one
        // fails right here (409) before it can create a second account.
        val accepted = invitations.save(invitation.accept(now))
        if (userRepository.findByEmail(invitation.email) != null) {
            throw UserAlreadyExistsException(invitation.email)
        }
        val user =
            userRepository.save(
                User(
                    email = invitation.email,
                    passwordHash = passwordHasher.hash(command.password),
                    name = command.name.trim(),
                    role = invitation.role,
                    // the invitation link went to this address and was used: the mailbox is proven
                    emailVerifiedAt = now,
                ),
            )
        auditLog.record(
            AuditEvent(
                actor = Actor(requireNotNull(user.id), user.role),
                action = AuditAction.STAFF_INVITATION_ACCEPTED,
                targetId = requireNotNull(accepted.id).toString(),
                before = invitation.toAuditSnapshot(),
                after = accepted.toAuditSnapshot(),
            ),
        )
        return user
    }

    // the same error for an unknown, expired, used or revoked token: the caller learns nothing about its state
    private fun pendingInvitation(
        token: String,
        now: Instant,
    ): StaffInvitation {
        val invitation =
            invitations.findByTokenHash(tokenService.hashToken(token))
                ?.takeIf { it.statusAt(now) == InvitationStatus.PENDING }
        return invitation ?: throw InvalidInvitationException()
    }
}
