package com.dbook.presentation.crm

import com.dbook.domain.crm.CustomerNote
import java.time.Instant

data class CustomerNoteResponse(
    val id: Long?,
    val authorId: Long,
    val body: String,
    val pinned: Boolean,
    val createdAt: Instant,
    val editedAt: Instant?,
) {
    companion object {
        fun from(note: CustomerNote) =
            CustomerNoteResponse(
                id = note.id,
                authorId = note.authorId,
                body = note.body,
                pinned = note.pinned,
                createdAt = note.createdAt,
                editedAt = note.editedAt,
            )
    }
}
