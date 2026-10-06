package com.dbook.application.crm

import com.dbook.domain.common.access.Actor
import com.dbook.domain.common.audit.AuditAction
import com.dbook.domain.common.audit.AuditEvent
import com.dbook.domain.common.audit.AuditLog
import com.dbook.domain.crm.CustomerErasure
import com.dbook.domain.identity.AnonymizedEmailRepository
import com.dbook.domain.identity.RefreshTokenRepository
import com.dbook.domain.identity.User
import com.dbook.domain.identity.UserRepository
import com.dbook.domain.identity.toAuditSnapshot
import org.springframework.stereotype.Service
import java.time.Clock

// The one place that anonymizes a customer, shared by the staff's request and the customer's own: they differ in who
// asks and how they prove it, not in what happens. Must run inside the caller's transaction.
@Service
class CustomerAnonymizer(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val anonymizedEmails: AnonymizedEmailRepository,
    private val erasure: CustomerErasure,
    private val auditLog: AuditLog,
    private val clock: Clock,
) {
    fun anonymize(
        actor: Actor,
        customer: User,
        reason: String,
    ): User {
        val customerId = requireNotNull(customer.id)
        val anonymized = customer.anonymize(clock.instant())
        anonymizedEmails.remember(customer.email)
        userRepository.save(anonymized)
        refreshTokenRepository.revokeAllForUser(customerId)
        erasure.erasePersonalData(customerId)
        auditLog.record(
            AuditEvent(
                actor = actor,
                action = AuditAction.CUSTOMER_ANONYMIZED,
                targetId = customerId.toString(),
                before = customer.toAuditSnapshot(),
                after = anonymized.toAuditSnapshot(),
                reason = reason,
            ),
        )
        return anonymized
    }
}
