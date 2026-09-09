package com.dbook.application.suggestflightsusecase

import com.dbook.application.SuggestFlightsUseCase
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

class ActiveOnlyFlightRepository(private val active: List<Flight>) : FlightRepository {
    override fun findById(id: Long): Flight? = active.find { it.id == id }

    override fun save(flight: Flight): Flight = flight

    override fun search(
        originIataCode: String,
        destinationIataCode: String,
        date: LocalDate,
    ): List<Flight> = emptyList()

    override fun findActive(): List<Flight> = active
}

class FixedAiSuggestionService(private val result: Result<AiSuggestionResult>) : AiSuggestionService {
    var lastCandidates: List<Flight>? = null

    override fun suggest(
        query: String,
        candidates: List<Flight>,
    ): AiSuggestionResult {
        lastCandidates = candidates
        return result.getOrThrow()
    }
}

class FakeAiSuggestionLogRepository : AiSuggestionLogRepository {
    val saved = mutableListOf<AiSuggestionLog>()

    override fun save(log: AiSuggestionLog): AiSuggestionLog {
        saved += log
        return log
    }
}

// Shared "given": one active flight (GRU-GIG) is the only candidate the AI service ever
// sees; each scenario below supplies its own AI outcome (success or failure).
abstract class SuggestFlightsUseCaseFixture {
    protected val flight =
        Flight(
            id = 1,
            title = "DB1234 GRU-GIG",
            price = BigDecimal("500.00"),
            totalCapacity = 180,
            availableCapacity = 170,
            flightNumber = "DB1234",
            origin = Airport(id = 1, iataCode = "GRU", name = "Guarulhos", city = "São Paulo", country = "Brasil"),
            destination =
                Airport(id = 2, iataCode = "GIG", name = "Galeão", city = "Rio de Janeiro", country = "Brasil"),
            departureTime = LocalDateTime.of(2027, 3, 1, 8, 0),
            arrivalTime = LocalDateTime.of(2027, 3, 1, 9, 10),
            seatClass = SeatClass.ECONOMY,
        )
    protected val flightRepository = ActiveOnlyFlightRepository(listOf(flight))
    protected val logRepository = FakeAiSuggestionLogRepository()

    protected fun useCase(aiService: AiSuggestionService) =
        SuggestFlightsUseCase(flightRepository, aiService, logRepository)
}
