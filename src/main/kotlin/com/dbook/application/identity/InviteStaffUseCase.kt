package com.dbook.application.identity

import com.dbook.application.common.afterCommit
import com.dbook.domain.audit.AuditAction
import com.dbook.domain.audit.AuditEvent
import com.dbook.domain.audit.AuditLog
import com.dbook.domain.identity.Actor
import com.dbook.domain.identity.InvitationTokenGenerator
import com.dbook.domain.identity.Role
import com.dbook.domain.identity.StaffInvitation
import com.dbook.domain.identity.StaffInvitationRepository
import com.dbook.domain.identity.TokenService
import com.dbook.domain.identity.UserAlreadyExistsException
import com.dbook.domain.identity.UserRepository
import com.dbook.domain.identity.toAuditSnapshot
import io.micrometer.observation.annotation.Observed
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

data class InviteStaffCommand(
    val actor: Actor,
    val email: String,
    val role: Role,
)

/** Invites someone to the staff. Inviting an address again replaces the open invitation with a new link. */
@Observed(name = "dbook.usecase")
@Service
class InviteStaffUseCase(
    private val invitations: StaffInvitationRepository,
    private val userRepository: UserRepository,
    private val tokenGenerator: InvitationTokenGenerator,
    private val tokenService: TokenService,
    private val mailer: InvitationMailer,
    private val auditLog: AuditLog,
    private val clock: Clock,
) {
    @Transactional
    fun execute(command: InviteStaffCommand): InvitationView {
        val email = command.email.trim().lowercase()
        if (userRepository.findByEmail(email) != null) {
            throw UserAlreadyExistsException(email)
        }
        val now = clock.instant()
        invitations.findOpenByEmail(email)?.let { invitations.save(it.revoke(now)) }

        val token = tokenGenerator.generate()
        val saved =
            invitations.save(
                StaffInvitation.issue(email, command.role, tokenService.hashToken(token), command.actor.id, now),
            )
        auditLog.record(
            AuditEvent(
                actor = command.actor,
                action = AuditAction.STAFF_INVITED,
                targetId = requireNotNull(saved.id).toString(),
                after = saved.toAuditSnapshot(),
            ),
        )
        // after the commit: if the transaction rolls back, no link goes out for an invitation that never existed
        afterCommit { mailer.send(saved, token) }
        return InvitationView(saved, saved.statusAt(now))
    }
}
