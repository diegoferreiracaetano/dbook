package com.dbook.application.identity

import com.dbook.application.common.afterCommit
import com.dbook.domain.audit.AuditAction
import com.dbook.domain.audit.AuditEvent
import com.dbook.domain.audit.AuditLog
import com.dbook.domain.identity.Actor
import com.dbook.domain.identity.InvitationNotFoundException
import com.dbook.domain.identity.InvitationTokenGenerator
import com.dbook.domain.identity.StaffInvitationRepository
import com.dbook.domain.identity.TokenService
import com.dbook.domain.identity.toAuditSnapshot
import io.micrometer.observation.annotation.Observed
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

data class ResendInvitationCommand(
    val actor: Actor,
    val invitationId: Long,
)

/** Sends an open invitation again with a NEW link (only the hash is stored, so the old token cannot be resent). */
@Observed(name = "dbook.usecase")
@Service
class ResendInvitationUseCase(
    private val invitations: StaffInvitationRepository,
    private val tokenGenerator: InvitationTokenGenerator,
    private val tokenService: TokenService,
    private val mailer: InvitationMailer,
    private val auditLog: AuditLog,
    private val clock: Clock,
) {
    @Transactional
    fun execute(command: ResendInvitationCommand): InvitationView {
        val invitation =
            invitations.findById(command.invitationId) ?: throw InvitationNotFoundException(command.invitationId)
        val now = clock.instant()
        val token = tokenGenerator.generate()
        val reissued = invitations.save(invitation.reissue(tokenService.hashToken(token), now))
        auditLog.record(
            AuditEvent(
                actor = command.actor,
                action = AuditAction.STAFF_INVITATION_RESENT,
                targetId = command.invitationId.toString(),
                before = invitation.toAuditSnapshot(),
                after = reissued.toAuditSnapshot(),
            ),
        )
        afterCommit { mailer.send(reissued, token) }
        return InvitationView(reissued, reissued.statusAt(now))
    }
}
