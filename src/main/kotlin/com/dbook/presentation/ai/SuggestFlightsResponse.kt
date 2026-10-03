package com.dbook.presentation.ai

import com.dbook.domain.ai.AiSuggestionResult

data class AiSuggestionItem(
    val flightId: Long,
    val reason: String,
)

data class SuggestFlightsResponse(
    val suggestions: List<AiSuggestionItem>,
) {
    companion object {
        fun from(result: AiSuggestionResult) =
            SuggestFlightsResponse(
                suggestions = result.suggestions.map { AiSuggestionItem(flightId = it.flightId, reason = it.reason) },
            )
    }
}
