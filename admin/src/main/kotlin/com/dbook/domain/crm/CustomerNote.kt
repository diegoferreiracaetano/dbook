package com.dbook.domain.crm

import com.dbook.domain.common.access.Actor
import com.dbook.domain.common.access.Role
import java.time.Instant

class CustomerNote(
    val id: Long? = null,
    val customerId: Long,
    val authorId: Long,
    val body: String,
    val pinned: Boolean = false,
    val createdAt: Instant,
    val editedAt: Instant? = null,
    val deletedAt: Instant? = null,
    val version: Long = 0,
) {
    init {
        require(body.isNotBlank()) { "note must not be blank" }
        require(body.length <= MAX_LENGTH) { "note must have at most $MAX_LENGTH characters" }
    }

    val isDeleted: Boolean get() = deletedAt != null

    // only the author rewrites a note: what a colleague wrote is not for anyone else to change
    fun edit(
        actor: Actor,
        newBody: String,
        newPinned: Boolean,
        now: Instant,
    ): CustomerNote {
        check(!isDeleted) { "A deleted note cannot be edited" }
        if (actor.id != authorId) {
            throw NoteAccessDeniedException()
        }
        return copy(body = newBody.trim(), pinned = newPinned, editedAt = now)
    }

    // the author may take their own note back; a SUPER_ADMIN may remove anyone's
    fun delete(
        actor: Actor,
        now: Instant,
    ): CustomerNote {
        check(!isDeleted) { "The note is already deleted" }
        if (actor.id != authorId && actor.role != Role.SUPER_ADMIN) {
            throw NoteAccessDeniedException()
        }
        return copy(deletedAt = now)
    }

    private fun copy(
        body: String = this.body,
        pinned: Boolean = this.pinned,
        editedAt: Instant? = this.editedAt,
        deletedAt: Instant? = this.deletedAt,
    ) = CustomerNote(id, customerId, authorId, body, pinned, createdAt, editedAt, deletedAt, version)

    companion object {
        const val MAX_LENGTH = 2000

        fun write(
            customerId: Long,
            author: Actor,
            body: String,
            pinned: Boolean,
            now: Instant,
        ) = CustomerNote(
            customerId = customerId,
            authorId = author.id,
            body = body.trim(),
            pinned = pinned,
            createdAt = now,
        )
    }
}
