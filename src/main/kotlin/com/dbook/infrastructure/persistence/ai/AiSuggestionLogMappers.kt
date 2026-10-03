package com.dbook.infrastructure.persistence.ai

import com.dbook.domain.ai.AiSuggestionLog

fun AiSuggestionLogJpaEntity.toDomain(): AiSuggestionLog =
    AiSuggestionLog(
        id = id,
        userId = userId,
        query = query,
        rawResponse = rawResponse,
        createdAt = createdAt,
    )

fun AiSuggestionLog.toJpaEntity(): AiSuggestionLogJpaEntity =
    AiSuggestionLogJpaEntity(
        id = id,
        userId = userId,
        query = query,
        rawResponse = rawResponse,
        createdAt = createdAt,
    )
