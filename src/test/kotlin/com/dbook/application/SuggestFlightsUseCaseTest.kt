package com.dbook.application

import com.dbook.domain.AiServiceUnavailableException
import com.dbook.domain.AiSuggestion
import com.dbook.domain.AiSuggestionLog
import com.dbook.domain.AiSuggestionLogRepository
import com.dbook.domain.AiSuggestionResult
import com.dbook.domain.AiSuggestionService
import com.dbook.domain.Airport
import com.dbook.domain.Flight
import com.dbook.domain.FlightRepository
import com.dbook.domain.SeatClass
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

private class ActiveOnlyFlightRepository(private val active: List<Flight>) : FlightRepository {
    override fun findById(id: Long): Flight? = active.find { it.id == id }

    override fun save(flight: Flight): Flight = flight

    override fun search(
        originIataCode: String,
        destinationIataCode: String,
        date: LocalDate,
    ): List<Flight> = emptyList()

    override fun findActive(): List<Flight> = active
}

private class FixedAiSuggestionService(private val result: Result<AiSuggestionResult>) : AiSuggestionService {
    var lastCandidates: List<Flight>? = null

    override fun suggest(
        query: String,
        candidates: List<Flight>,
    ): AiSuggestionResult {
        lastCandidates = candidates
        return result.getOrThrow()
    }
}

private class FakeAiSuggestionLogRepository : AiSuggestionLogRepository {
    val saved = mutableListOf<AiSuggestionLog>()

    override fun save(log: AiSuggestionLog): AiSuggestionLog {
        saved += log
        return log
    }
}

class SuggestFlightsUseCaseTest {
    private val gru = Airport(id = 1, iataCode = "GRU", name = "Guarulhos", city = "São Paulo", country = "Brasil")
    private val gig = Airport(id = 2, iataCode = "GIG", name = "Galeão", city = "Rio de Janeiro", country = "Brasil")
    private val flight =
        Flight(
            id = 1,
            title = "DB1234 GRU-GIG",
            price = BigDecimal("500.00"),
            totalCapacity = 180,
            availableCapacity = 170,
            flightNumber = "DB1234",
            origin = gru,
            destination = gig,
            departureTime = LocalDateTime.of(2027, 3, 1, 8, 0),
            arrivalTime = LocalDateTime.of(2027, 3, 1, 9, 10),
            seatClass = SeatClass.ECONOMY,
        )
    private val flightRepository = ActiveOnlyFlightRepository(listOf(flight))
    private val logRepository = FakeAiSuggestionLogRepository()

    @Test
    fun `passes active flights as context and logs the raw response on success`() {
        val aiResult =
            AiSuggestionResult(
                suggestions = listOf(AiSuggestion(flightId = 1, reason = "cheapest match")),
                rawResponse = "{}",
            )
        val aiService = FixedAiSuggestionService(Result.success(aiResult))
        val useCase = SuggestFlightsUseCase(flightRepository, aiService, logRepository)

        val result = useCase.execute(SuggestFlightsCommand(query = "cheap flights to Rio", requestingUserId = 42))

        assertEquals(aiResult, result)
        assertEquals(listOf(flight), aiService.lastCandidates)
        assertEquals(1, logRepository.saved.size)
        assertEquals("{}", logRepository.saved.single().rawResponse)
        assertEquals(42, logRepository.saved.single().userId)
    }

    @Test
    fun `still logs the attempt when the AI call fails, then rethrows`() {
        val aiService = FixedAiSuggestionService(Result.failure(AiServiceUnavailableException("Bedrock is down")))
        val useCase = SuggestFlightsUseCase(flightRepository, aiService, logRepository)

        assertFailsWith<AiServiceUnavailableException> {
            useCase.execute(SuggestFlightsCommand(query = "cheap flights to Rio", requestingUserId = 42))
        }

        assertEquals(1, logRepository.saved.size)
        assertEquals("ERROR: Bedrock is down", logRepository.saved.single().rawResponse)
    }
}
