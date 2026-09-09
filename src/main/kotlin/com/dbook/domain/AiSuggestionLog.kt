package com.dbook.domain

import java.time.LocalDateTime

// Audit trail for every AI call — the model only ever suggests, it never books or
// decides on its own, but every prompt/response pair still needs to be traceable.
data class AiSuggestionLog(
    val id: Long? = null,
    val userId: Long,
    val query: String,
    val rawResponse: String,
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
