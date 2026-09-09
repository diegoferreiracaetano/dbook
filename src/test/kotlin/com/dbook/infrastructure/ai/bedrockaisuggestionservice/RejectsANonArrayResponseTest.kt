package com.dbook.infrastructure.ai.bedrockaisuggestionservice

import com.dbook.domain.AiResponseParsingException
import kotlin.test.Test
import kotlin.test.assertFailsWith

class RejectsANonArrayResponseTest : BedrockAiSuggestionServiceFixture() {
    @Test
    fun `given a JSON object instead of an array when parsing then it throws AiResponseParsingException`() {
        assertFailsWith<AiResponseParsingException> {
            service.parseSuggestions("""{"flightId":7,"reason":"not wrapped in an array"}""")
        }
    }
}
