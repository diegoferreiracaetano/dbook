package com.dbook.domain.ai

import com.dbook.domain.catalog.Flight

/**
 * Suggests flights matching a natural-language [query], grounded in a real [candidates]
 * pool — it never invents or books anything, only ranks/explains what's already
 * available (see `BedrockAiSuggestionService` for the Bedrock/Claude implementation).
 */
interface AiSuggestionService {
    fun suggest(
        query: String,
        candidates: List<Flight>,
    ): AiSuggestionResult
}
