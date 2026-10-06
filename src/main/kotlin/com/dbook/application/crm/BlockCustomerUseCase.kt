package com.dbook.application.crm

import com.dbook.application.identity.BlockUserCommand
import com.dbook.application.identity.BlockUserUseCase
import com.dbook.domain.common.access.Actor
import com.dbook.domain.common.audit.AuditAction
import com.dbook.domain.common.audit.AuditEvent
import com.dbook.domain.common.audit.AuditLog
import com.dbook.domain.identity.User
import com.dbook.domain.identity.toAuditSnapshot
import io.micrometer.observation.annotation.Observed
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class BlockCustomerCommand(
    val actor: Actor,
    val customerId: Long,
    val reason: String,
)

@Observed(name = "dbook.usecase")
@Service
class BlockCustomerUseCase(
    private val customerGuard: CustomerGuard,
    private val blockUserUseCase: BlockUserUseCase,
    private val auditLog: AuditLog,
) {
    @Transactional
    fun execute(command: BlockCustomerCommand): User {
        val reason = command.reason.trim()
        require(reason.length >= MIN_REASON_LENGTH) { "reason must have at least $MIN_REASON_LENGTH characters" }
        val before = customerGuard.customer(command.customerId)

        val blocked = blockUserUseCase.execute(BlockUserCommand(command.customerId, reason))
        auditLog.record(
            AuditEvent(
                actor = command.actor,
                action = AuditAction.CUSTOMER_BLOCKED,
                targetId = command.customerId.toString(),
                before = before.toAuditSnapshot(),
                after = blocked.toAuditSnapshot(),
                reason = reason,
            ),
        )
        return blocked
    }

    private companion object {
        const val MIN_REASON_LENGTH = 10
    }
}
