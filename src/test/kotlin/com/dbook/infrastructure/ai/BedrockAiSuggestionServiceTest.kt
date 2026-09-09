package com.dbook.infrastructure.ai

import com.dbook.domain.AiResponseParsingException
import com.dbook.domain.Airport
import com.dbook.domain.Flight
import com.dbook.domain.SeatClass
import com.fasterxml.jackson.databind.ObjectMapper
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

// Only buildRequestBody/extractText/parseSuggestions are exercised here — none of them
// touch the network, so a real (but never-invoked) client is fine; building one doesn't
// validate credentials eagerly. Real Bedrock calls can't be tested without a live AWS
// session (see CHECKLIST.md).
class BedrockAiSuggestionServiceTest {
    private val client = BedrockRuntimeClient.builder().region(Region.US_EAST_1).build()
    private val service = BedrockAiSuggestionService(client, ObjectMapper(), modelId = "test-model")

    private val flight =
        Flight(
            id = 7,
            title = "DB1234 GRU-GIG",
            price = BigDecimal("500.00"),
            totalCapacity = 180,
            availableCapacity = 170,
            flightNumber = "DB1234",
            origin = Airport(1, "GRU", "Guarulhos", "São Paulo", "Brasil"),
            destination = Airport(2, "GIG", "Galeão", "Rio de Janeiro", "Brasil"),
            departureTime = LocalDateTime.of(2027, 3, 1, 8, 0),
            arrivalTime = LocalDateTime.of(2027, 3, 1, 9, 10),
            seatClass = SeatClass.ECONOMY,
        )

    @Test
    fun `builds a Bedrock-shaped request body containing the flight context`() {
        val body = service.buildRequestBody("cheap flights to Rio", listOf(flight))

        assertTrue(body.contains("\"anthropic_version\":\"bedrock-2023-05-31\""))
        assertTrue(body.contains("id=7"))
        assertTrue(body.contains("cheap flights to Rio"))
    }

    @Test
    fun `extracts the text block from a Claude-on-Bedrock response`() {
        val rawResponse = """{"content":[{"type":"text","text":"[{\"flightId\":7,\"reason\":\"cheapest\"}]"}]}"""

        val text = service.extractText(rawResponse)

        assertEquals("""[{"flightId":7,"reason":"cheapest"}]""", text)
    }

    @Test
    fun `rejects a Bedrock response with no text content`() {
        assertFailsWith<AiResponseParsingException> {
            service.extractText("""{"content":[]}""")
        }
    }

    @Test
    fun `parses a well-formed suggestion array`() {
        val suggestions = service.parseSuggestions("""[{"flightId":7,"reason":"cheapest match"}]""")

        assertEquals(7L, suggestions.single().flightId)
        assertEquals("cheapest match", suggestions.single().reason)
    }

    @Test
    fun `rejects a suggestion missing flightId`() {
        assertFailsWith<AiResponseParsingException> {
            service.parseSuggestions("""[{"reason":"no id here"}]""")
        }
    }

    @Test
    fun `rejects a non-array AI response`() {
        assertFailsWith<AiResponseParsingException> {
            service.parseSuggestions("""{"flightId":7,"reason":"not wrapped in an array"}""")
        }
    }

    @Test
    fun `rejects an AI response that isn't valid JSON at all`() {
        assertFailsWith<AiResponseParsingException> {
            service.parseSuggestions("sure, here are some flights: 7 and 12")
        }
    }
}
