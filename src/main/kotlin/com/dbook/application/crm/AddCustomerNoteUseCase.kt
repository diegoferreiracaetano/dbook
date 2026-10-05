package com.dbook.application.crm

import com.dbook.domain.audit.AuditAction
import com.dbook.domain.audit.AuditEvent
import com.dbook.domain.audit.AuditLog
import com.dbook.domain.crm.CustomerNote
import com.dbook.domain.crm.CustomerNoteRepository
import com.dbook.domain.crm.toAuditSnapshot
import com.dbook.domain.identity.Actor
import io.micrometer.observation.annotation.Observed
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

data class AddCustomerNoteCommand(
    val actor: Actor,
    val customerId: Long,
    val body: String,
    val pinned: Boolean,
)

@Observed(name = "dbook.usecase")
@Service
class AddCustomerNoteUseCase(
    private val customerGuard: CustomerGuard,
    private val notes: CustomerNoteRepository,
    private val auditLog: AuditLog,
    private val clock: Clock,
) {
    @Transactional
    fun execute(command: AddCustomerNoteCommand): CustomerNote {
        customerGuard.customer(command.customerId)
        val note =
            notes.save(
                CustomerNote.write(command.customerId, command.actor, command.body, command.pinned, clock.instant()),
            )
        auditLog.record(
            AuditEvent(
                actor = command.actor,
                action = AuditAction.CUSTOMER_NOTE_ADDED,
                targetId = requireNotNull(note.id).toString(),
                after = note.toAuditSnapshot(),
            ),
        )
        return note
    }
}
