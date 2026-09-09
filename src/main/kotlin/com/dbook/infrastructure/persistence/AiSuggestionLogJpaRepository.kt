package com.dbook.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface AiSuggestionLogJpaRepository : JpaRepository<AiSuggestionLogJpaEntity, Long>
