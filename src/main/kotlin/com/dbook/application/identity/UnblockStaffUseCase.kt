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

data class UnblockStaffCommand(
    val actor: Actor,
    val targetId: Long,
)

@Observed(name = "dbook.usecase")
@Service
class UnblockStaffUseCase(
    private val unblockUserUseCase: UnblockUserUseCase,
    private val safeguards: StaffSafeguards,
    private val auditLog: AuditLog,
) {
    @Transactional
    fun execute(command: UnblockStaffCommand): User {
        val target = safeguards.staffMember(command.targetId)
        val unblocked = unblockUserUseCase.execute(command.targetId)
        auditLog.record(
            AuditEvent(
                actor = command.actor,
                action = AuditAction.STAFF_UNBLOCKED,
                targetId = command.targetId.toString(),
                before = target.toAuditSnapshot(),
                after = unblocked.toAuditSnapshot(),
            ),
        )
        return unblocked
    }
}
