package com.dbook.application.crm

import com.dbook.domain.audit.AuditAction
import com.dbook.domain.audit.AuditEvent
import com.dbook.domain.audit.AuditLog
import com.dbook.domain.crm.CustomerNoteNotFoundException
import com.dbook.domain.crm.CustomerNoteRepository
import com.dbook.domain.crm.toAuditSnapshot
import com.dbook.domain.identity.Actor
import io.micrometer.observation.annotation.Observed
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

data class DeleteCustomerNoteCommand(
    val actor: Actor,
    val customerId: Long,
    val noteId: Long,
)

// logical deletion: the row stays (for the trail) but no list or edit sees it again
@Observed(name = "dbook.usecase")
@Service
class DeleteCustomerNoteUseCase(
    private val notes: CustomerNoteRepository,
    private val auditLog: AuditLog,
    private val clock: Clock,
) {
    @Transactional
    fun execute(command: DeleteCustomerNoteCommand) {
        val note =
            notes.findActive(command.customerId, command.noteId) ?: throw CustomerNoteNotFoundException(command.noteId)
        val deleted = notes.save(note.delete(command.actor, clock.instant()))
        auditLog.record(
            AuditEvent(
                actor = command.actor,
                action = AuditAction.CUSTOMER_NOTE_DELETED,
                targetId = command.noteId.toString(),
                before = note.toAuditSnapshot(),
                after = deleted.toAuditSnapshot(),
            ),
        )
    }
}
