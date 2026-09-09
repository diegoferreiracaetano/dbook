package com.dbook.infrastructure.ai.bedrockaisuggestionservice

import com.dbook.domain.Airport
import com.dbook.domain.Flight
import com.dbook.domain.SeatClass
import com.dbook.infrastructure.ai.BedrockAiSuggestionService
import com.fasterxml.jackson.databind.ObjectMapper
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient
import java.math.BigDecimal
import java.time.LocalDateTime

// Only buildRequestBody/extractText/parseSuggestions (internal, not private — see
// BedrockAiSuggestionService) are exercised across this whole package — none of them
// touch the network, so a real (but never-invoked) client is fine; building one doesn't
// validate credentials eagerly. Real Bedrock calls can't be tested without a live AWS
// session (see CHECKLIST.md).
abstract class BedrockAiSuggestionServiceFixture {
    private val client = BedrockRuntimeClient.builder().region(Region.US_EAST_1).build()
    protected val service = BedrockAiSuggestionService(client, ObjectMapper(), modelId = "test-model")

    protected val flight =
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
}
