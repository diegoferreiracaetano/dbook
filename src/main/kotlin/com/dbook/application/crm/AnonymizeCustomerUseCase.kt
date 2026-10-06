package com.dbook.application.crm

import com.dbook.domain.common.access.Actor
import com.dbook.domain.identity.User
import io.micrometer.observation.annotation.Observed
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class AnonymizeCustomerCommand(
    val actor: Actor,
    val customerId: Long,
    val reason: String,
    val confirmation: String,
)

// Irreversible, so it asks twice: a reason for the trail and a phrase that names the customer, which a wrong id
// cannot satisfy.
@Observed(name = "dbook.usecase")
@Service
class AnonymizeCustomerUseCase(
    private val customerGuard: CustomerGuard,
    private val customerAnonymizer: CustomerAnonymizer,
) {
    @Transactional
    fun execute(command: AnonymizeCustomerCommand): User {
        val reason = command.reason.trim()
        require(reason.length >= MIN_REASON_LENGTH) { "reason must have at least $MIN_REASON_LENGTH characters" }
        require(command.confirmation == confirmationFor(command.customerId)) {
            "confirmation must be exactly '${confirmationFor(command.customerId)}'"
        }
        return customerAnonymizer.anonymize(command.actor, customerGuard.customer(command.customerId), reason)
    }

    companion object {
        const val MIN_REASON_LENGTH = 10

        fun confirmationFor(customerId: Long) = "ANONYMIZE $customerId"
    }
}
