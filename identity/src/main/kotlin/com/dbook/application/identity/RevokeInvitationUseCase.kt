package com.dbook.application.identity

import com.dbook.domain.common.access.Actor
import com.dbook.domain.common.audit.AuditAction
import com.dbook.domain.common.audit.AuditEvent
import com.dbook.domain.common.audit.AuditLog
import com.dbook.domain.identity.InvitationNotFoundException
import com.dbook.domain.identity.StaffInvitationRepository
import com.dbook.domain.identity.toAuditSnapshot
import io.micrometer.observation.annotation.Observed
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

data class RevokeInvitationCommand(
    val actor: Actor,
    val invitationId: Long,
)

@Observed(name = "dbook.usecase")
@Service
class RevokeInvitationUseCase(
    private val invitations: StaffInvitationRepository,
    private val auditLog: AuditLog,
    private val clock: Clock,
) {
    @Transactional
    fun execute(command: RevokeInvitationCommand) {
        val invitation =
            invitations.findById(command.invitationId) ?: throw InvitationNotFoundException(command.invitationId)
        val revoked = invitations.save(invitation.revoke(clock.instant()))
        auditLog.record(
            AuditEvent(
                actor = command.actor,
                action = AuditAction.STAFF_INVITATION_REVOKED,
                targetId = command.invitationId.toString(),
                before = invitation.toAuditSnapshot(),
                after = revoked.toAuditSnapshot(),
            ),
        )
    }
}
