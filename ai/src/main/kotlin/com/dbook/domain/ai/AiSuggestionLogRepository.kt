package com.dbook.domain.ai

/** Persistence port for [AiSuggestionLog] — every AI call is audited, success or failure. */
interface AiSuggestionLogRepository {
    fun save(log: AiSuggestionLog): AiSuggestionLog
}
