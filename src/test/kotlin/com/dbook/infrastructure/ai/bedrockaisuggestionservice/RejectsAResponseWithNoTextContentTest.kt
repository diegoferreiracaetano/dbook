package com.dbook.infrastructure.ai.bedrockaisuggestionservice

import com.dbook.domain.AiResponseParsingException
import kotlin.test.Test
import kotlin.test.assertFailsWith

class RejectsAResponseWithNoTextContentTest : BedrockAiSuggestionServiceFixture() {
    @Test
    fun `given an empty content array when extracting text then it throws AiResponseParsingException`() {
        assertFailsWith<AiResponseParsingException> {
            service.extractText("""{"content":[]}""")
        }
    }
}
