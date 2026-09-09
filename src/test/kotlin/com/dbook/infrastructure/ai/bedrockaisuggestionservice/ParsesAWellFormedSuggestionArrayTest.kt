package com.dbook.infrastructure.ai.bedrockaisuggestionservice

import kotlin.test.Test
import kotlin.test.assertEquals

class ParsesAWellFormedSuggestionArrayTest : BedrockAiSuggestionServiceFixture() {
    @Test
    fun `given a valid JSON array when parsing suggestions then they map to AiSuggestion values`() {
        val suggestions = service.parseSuggestions("""[{"flightId":7,"reason":"cheapest match"}]""")

        assertEquals(7L, suggestions.single().flightId)
        assertEquals("cheapest match", suggestions.single().reason)
    }
}
