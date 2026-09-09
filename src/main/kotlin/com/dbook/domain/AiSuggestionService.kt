package com.dbook.domain

interface AiSuggestionService {
    fun suggest(
        query: String,
        candidates: List<Flight>,
    ): AiSuggestionResult
}
