package com.dbook.infrastructure.ai.bedrockaisuggestionservice

import com.dbook.domain.AiResponseParsingException
import kotlin.test.Test
import kotlin.test.assertFailsWith

class RejectsASuggestionMissingFlightIdTest : BedrockAiSuggestionServiceFixture() {
    @Test
    fun `given a suggestion missing flightId when parsing then it throws AiResponseParsingException`() {
        assertFailsWith<AiResponseParsingException> {
            service.parseSuggestions("""[{"reason":"no id here"}]""")
        }
    }
}
