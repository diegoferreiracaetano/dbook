package com.dbook.application.crm

import com.dbook.domain.audit.AuditAction
import com.dbook.domain.audit.AuditEvent
import com.dbook.domain.audit.AuditFilter
import com.dbook.domain.audit.AuditLog
import com.dbook.domain.audit.AuditLogReader
import com.dbook.domain.crm.CustomerProfile
import com.dbook.domain.crm.CustomerProfileReader
import com.dbook.domain.identity.Actor
import com.dbook.domain.identity.UserNotFoundException
import io.micrometer.observation.annotation.Observed
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Duration

// Opening the full picture of a customer is access to personal data, so it leaves a trail. The same person
// reopening the same customer within a few minutes is one access, not a hundred rows of noise.
@Observed(name = "dbook.usecase")
@Service
class GetCustomerProfileUseCase(
    private val customerProfileReader: CustomerProfileReader,
    private val auditLogReader: AuditLogReader,
    private val auditLog: AuditLog,
    private val clock: Clock,
) {
    fun execute(
        actor: Actor,
        customerId: Long,
    ): CustomerProfile {
        val profile = customerProfileReader.find(customerId) ?: throw UserNotFoundException(customerId)
        recordViewOnce(actor, customerId)
        return profile
    }

    private fun recordViewOnce(
        actor: Actor,
        customerId: Long,
    ) {
        val recent =
            AuditFilter(
                actorId = actor.id,
                action = AuditAction.CUSTOMER_VIEWED,
                targetId = customerId.toString(),
                from = clock.instant().minus(SAME_ACCESS_WINDOW),
            )
        if (auditLogReader.search(recent, after = null, limit = 1).entries.isEmpty()) {
            auditLog.record(AuditEvent(actor, AuditAction.CUSTOMER_VIEWED, customerId.toString()))
        }
    }

    companion object {
        val SAME_ACCESS_WINDOW: Duration = Duration.ofMinutes(5)
    }
}
