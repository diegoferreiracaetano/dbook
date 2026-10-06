package com.dbook.application.crm

import com.dbook.domain.common.access.Actor
import com.dbook.domain.common.audit.AuditAction
import com.dbook.domain.common.audit.AuditEvent
import com.dbook.domain.common.audit.AuditLog
import com.dbook.domain.crm.CustomerNote
import com.dbook.domain.crm.CustomerNoteNotFoundException
import com.dbook.domain.crm.CustomerNoteRepository
import com.dbook.domain.crm.toAuditSnapshot
import io.micrometer.observation.annotation.Observed
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

data class EditCustomerNoteCommand(
    val actor: Actor,
    val customerId: Long,
    val noteId: Long,
    val body: String?,
    val pinned: Boolean?,
)

@Observed(name = "dbook.usecase")
@Service
class EditCustomerNoteUseCase(
    private val notes: CustomerNoteRepository,
    private val auditLog: AuditLog,
    private val clock: Clock,
) {
    @Transactional
    fun execute(command: EditCustomerNoteCommand): CustomerNote {
        require(command.body != null || command.pinned != null) { "body or pinned must be sent" }
        val note =
            notes.findActive(command.customerId, command.noteId) ?: throw CustomerNoteNotFoundException(command.noteId)
        val edited =
            notes.save(
                note.edit(command.actor, command.body ?: note.body, command.pinned ?: note.pinned, clock.instant()),
            )
        auditLog.record(
            AuditEvent(
                actor = command.actor,
                action = AuditAction.CUSTOMER_NOTE_EDITED,
                targetId = command.noteId.toString(),
                before = note.toAuditSnapshot(),
                after = edited.toAuditSnapshot(),
            ),
        )
        return edited
    }
}
