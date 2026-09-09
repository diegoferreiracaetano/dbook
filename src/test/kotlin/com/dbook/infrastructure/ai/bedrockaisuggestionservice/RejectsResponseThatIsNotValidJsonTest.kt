package com.dbook.infrastructure.ai.bedrockaisuggestionservice

import com.dbook.domain.AiResponseParsingException
import kotlin.test.Test
import kotlin.test.assertFailsWith

class RejectsResponseThatIsNotValidJsonTest : BedrockAiSuggestionServiceFixture() {
    @Test
    fun `given free-form prose instead of JSON when parsing then it throws AiResponseParsingException`() {
        assertFailsWith<AiResponseParsingException> {
            service.parseSuggestions("sure, here are some flights: 7 and 12")
        }
    }
}
