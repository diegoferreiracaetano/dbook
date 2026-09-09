package com.dbook.infrastructure.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "ai_suggestion_log")
class AiSuggestionLogJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(name = "user_id", nullable = false)
    val userId: Long,
    @Column(nullable = false, columnDefinition = "TEXT")
    val query: String,
    @Column(name = "raw_response", nullable = false, columnDefinition = "TEXT")
    val rawResponse: String,
    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime,
)
