package com.dbook.infrastructure.ai.bedrockaisuggestionservice

import kotlin.test.Test
import kotlin.test.assertEquals

class ExtractsTheTextBlockFromAClaudeResponseTest : BedrockAiSuggestionServiceFixture() {
    @Test
    fun `given a well-formed Bedrock response when extracting text then the inner content is unwrapped`() {
        val rawResponse = """{"content":[{"type":"text","text":"[{\"flightId\":7,\"reason\":\"cheapest\"}]"}]}"""

        val text = service.extractText(rawResponse)

        assertEquals("""[{"flightId":7,"reason":"cheapest"}]""", text)
    }
}
