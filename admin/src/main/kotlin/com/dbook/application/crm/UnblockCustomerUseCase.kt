package com.dbook.application.crm

import com.dbook.application.identity.UnblockUserUseCase
import com.dbook.domain.common.access.Actor
import com.dbook.domain.common.audit.AuditAction
import com.dbook.domain.common.audit.AuditEvent
import com.dbook.domain.common.audit.AuditLog
import com.dbook.domain.identity.User
import com.dbook.domain.identity.toAuditSnapshot
import io.micrometer.observation.annotation.Observed
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class UnblockCustomerCommand(
    val actor: Actor,
    val customerId: Long,
)

@Observed(name = "dbook.usecase")
@Service
class UnblockCustomerUseCase(
    private val customerGuard: CustomerGuard,
    private val unblockUserUseCase: UnblockUserUseCase,
    private val auditLog: AuditLog,
) {
    @Transactional
    fun execute(command: UnblockCustomerCommand): User {
        val before = customerGuard.customer(command.customerId)

        val unblocked = unblockUserUseCase.execute(command.customerId)
        auditLog.record(
            AuditEvent(
                actor = command.actor,
                action = AuditAction.CUSTOMER_UNBLOCKED,
                targetId = command.customerId.toString(),
                before = before.toAuditSnapshot(),
                after = unblocked.toAuditSnapshot(),
            ),
        )
        return unblocked
    }
}
