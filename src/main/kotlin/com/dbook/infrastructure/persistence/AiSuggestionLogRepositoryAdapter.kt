package com.dbook.infrastructure.persistence

import com.dbook.domain.AiSuggestionLog
import com.dbook.domain.AiSuggestionLogRepository
import org.springframework.stereotype.Repository

@Repository
class AiSuggestionLogRepositoryAdapter(
    private val jpaRepository: AiSuggestionLogJpaRepository,
) : AiSuggestionLogRepository {
    override fun save(log: AiSuggestionLog): AiSuggestionLog = jpaRepository.save(log.toJpaEntity()).toDomain()
}
