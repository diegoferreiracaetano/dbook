package com.dbook.infrastructure.persistence.ai

import com.dbook.domain.ai.AiSuggestionLog
import com.dbook.domain.ai.AiSuggestionLogRepository
import org.springframework.stereotype.Repository

@Repository
class AiSuggestionLogRepositoryAdapter(
    private val jpaRepository: AiSuggestionLogJpaRepository,
) : AiSuggestionLogRepository {
    override fun save(log: AiSuggestionLog): AiSuggestionLog = jpaRepository.save(log.toJpaEntity()).toDomain()
}
