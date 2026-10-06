package com.dbook.domain.ai

/** One suggested [com.dbook.domain.flight.Flight], with the model's stated reason for it. */
data class AiSuggestion(
    val flightId: Long,
    val reason: String,
)

/** [suggestions] parsed from the model's completion; [rawResponse] kept for [AiSuggestionLog] auditing. */
data class AiSuggestionResult(
    val suggestions: List<AiSuggestion>,
    val rawResponse: String,
)
