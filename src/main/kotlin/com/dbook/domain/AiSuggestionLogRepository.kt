package com.dbook.domain

interface AiSuggestionLogRepository {
    fun save(log: AiSuggestionLog): AiSuggestionLog
}
