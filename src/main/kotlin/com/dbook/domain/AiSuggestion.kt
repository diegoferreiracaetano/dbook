package com.dbook.domain

data class AiSuggestion(
    val flightId: Long,
    val reason: String,
)

data class AiSuggestionResult(
    val suggestions: List<AiSuggestion>,
    val rawResponse: String,
)
