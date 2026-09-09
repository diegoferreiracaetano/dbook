package com.dbook.infrastructure.ai.bedrockaisuggestionservice

import kotlin.test.Test
import kotlin.test.assertTrue

class BuildsARequestBodyContainingTheFlightContextTest : BedrockAiSuggestionServiceFixture() {
    @Test
    fun `given a query and a candidate flight when building the request body then both are embedded`() {
        val body = service.buildRequestBody("cheap flights to Rio", listOf(flight))

        assertTrue(body.contains("\"anthropic_version\":\"bedrock-2023-05-31\""))
        assertTrue(body.contains("id=7"))
        assertTrue(body.contains("cheap flights to Rio"))
    }
}
