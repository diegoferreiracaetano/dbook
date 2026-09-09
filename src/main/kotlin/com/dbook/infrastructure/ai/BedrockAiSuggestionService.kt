package com.dbook.infrastructure.ai

import com.dbook.domain.AiResponseParsingException
import com.dbook.domain.AiServiceUnavailableException
import com.dbook.domain.AiSuggestion
import com.dbook.domain.AiSuggestionResult
import com.dbook.domain.AiSuggestionService
import com.dbook.domain.Flight
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.core.exception.SdkException
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest

@Component
class BedrockAiSuggestionService(
    private val bedrockRuntimeClient: BedrockRuntimeClient,
    private val objectMapper: ObjectMapper,
    @Value("\${ai.bedrock.model-id}") private val modelId: String,
) : AiSuggestionService {
    override fun suggest(
        query: String,
        candidates: List<Flight>,
    ): AiSuggestionResult {
        val requestBody = buildRequestBody(query, candidates)
        val rawResponse =
            try {
                val response =
                    bedrockRuntimeClient.invokeModel(
                        InvokeModelRequest.builder()
                            .modelId(modelId)
                            .contentType("application/json")
                            .accept("application/json")
                            .body(SdkBytes.fromUtf8String(requestBody))
                            .build(),
                    )
                response.body().asUtf8String()
            } catch (e: SdkException) {
                throw AiServiceUnavailableException("Bedrock call failed: ${e.message}", e)
            }
        val text = extractText(rawResponse)
        return AiSuggestionResult(parseSuggestions(text), rawResponse)
    }

    internal fun buildRequestBody(
        query: String,
        candidates: List<Flight>,
    ): String {
        val prompt = buildPrompt(query, candidates)
        return objectMapper.writeValueAsString(
            mapOf(
                "anthropic_version" to "bedrock-2023-05-31",
                "max_tokens" to MAX_RESPONSE_TOKENS,
                "messages" to listOf(mapOf("role" to "user", "content" to prompt)),
            ),
        )
    }

    private fun buildPrompt(
        query: String,
        candidates: List<Flight>,
    ): String {
        val flightLines =
            candidates.joinToString("\n") { flight ->
                "id=${flight.id}, ${flight.flightNumber}, ${flight.origin.iataCode}->${flight.destination.iataCode}, " +
                    "departure=${flight.departureTime}, price=${flight.price}, seatClass=${flight.seatClass}, " +
                    "availableCapacity=${flight.availableCapacity}"
            }
        return """
            You are a flight suggestion assistant for a booking platform. Given the user's request and the
            list of available flights below, suggest the flights that best match. Never invent a flight id
            that isn't in the list below, and never suggest a flight with availableCapacity of 0.
            Respond ONLY with a JSON array, no prose before or after it, in this exact shape:
            [{"flightId": <id>, "reason": "<short reason, in the same language as the user's request>"}]

            User request: "$query"

            Available flights:
            $flightLines
            """.trimIndent()
    }

    internal fun extractText(rawResponse: String): String {
        val root =
            try {
                objectMapper.readTree(rawResponse)
            } catch (e: JsonProcessingException) {
                throw AiResponseParsingException("Bedrock response was not valid JSON: $rawResponse", e)
            }
        return root["content"]?.firstOrNull()?.get("text")?.asText()
            ?: throw AiResponseParsingException("Bedrock response had no text content: $rawResponse")
    }

    internal fun parseSuggestions(text: String): List<AiSuggestion> {
        val node =
            try {
                objectMapper.readTree(text)
            } catch (e: JsonProcessingException) {
                throw AiResponseParsingException("AI response was not valid JSON: $text", e)
            }
        if (!node.isArray) {
            throw AiResponseParsingException("Expected a JSON array from the AI, got: $text")
        }
        return node.map(::toSuggestion)
    }

    private fun toSuggestion(item: JsonNode): AiSuggestion =
        AiSuggestion(
            flightId =
                item["flightId"]?.takeIf { it.isNumber }?.asLong()
                    ?: throw AiResponseParsingException("Missing/invalid flightId in AI suggestion: $item"),
            reason =
                item["reason"]?.asText()
                    ?: throw AiResponseParsingException("Missing reason in AI suggestion: $item"),
        )

    companion object {
        private const val MAX_RESPONSE_TOKENS = 1024
    }
}
