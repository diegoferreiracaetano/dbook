package com.dbook.infrastructure.persistence.ai

import org.springframework.data.jpa.repository.JpaRepository

interface AiSuggestionLogJpaRepository : JpaRepository<AiSuggestionLogJpaEntity, Long>
