package com.dbook.application.identity

import com.dbook.domain.common.access.Actor
import com.dbook.domain.common.audit.AuditAction
import com.dbook.domain.common.audit.AuditEvent
import com.dbook.domain.common.audit.AuditLog
import com.dbook.domain.identity.User
import com.dbook.domain.identity.toAuditSnapshot
import io.micrometer.observation.annotation.Observed
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class BlockStaffCommand(
    val actor: Actor,
    val targetId: Long,
    val reason: String,
)

@Observed(name = "dbook.usecase")
@Service
class BlockStaffUseCase(
    private val blockUserUseCase: BlockUserUseCase,
    private val safeguards: StaffSafeguards,
    private val auditLog: AuditLog,
) {
    @Transactional
    fun execute(command: BlockStaffCommand): User {
        safeguards.requireNotSelf(command.actor, command.targetId)
        val target = safeguards.staffMember(command.targetId)
        safeguards.requireSuperAdminRemainsWithout(target)

        val blocked = blockUserUseCase.execute(BlockUserCommand(command.targetId, command.reason))
        auditLog.record(
            AuditEvent(
                actor = command.actor,
                action = AuditAction.STAFF_BLOCKED,
                targetId = command.targetId.toString(),
                before = target.toAuditSnapshot(),
                after = blocked.toAuditSnapshot(),
                reason = command.reason.trim(),
            ),
        )
        return blocked
    }
}
