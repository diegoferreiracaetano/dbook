package com.dbook.application.ai.suggestflightsusecase

import com.dbook.application.ai.SuggestFlightsUseCase
import com.dbook.domain.ai.AiSuggestionLog
import com.dbook.domain.ai.AiSuggestionLogRepository
import com.dbook.domain.ai.AiSuggestionResult
import com.dbook.domain.ai.AiSuggestionService
import com.dbook.domain.catalog.Airline
import com.dbook.domain.catalog.Airport
import com.dbook.domain.catalog.Flight
import com.dbook.domain.catalog.FlightRepository
import com.dbook.domain.catalog.SeatClass
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

    override fun findLowestPrice(
        destinationIataCode: String,
        from: LocalDate,
        to: LocalDate,
    ): BigDecimal? = error("not used by SuggestFlightsUseCase")

    override fun update(
        flight: Flight,
        expectedVersion: Long?,
    ): Flight = error("not needed for this test")
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
            airline = Airline(id = 1, iataCode = "LA", name = "LATAM Airlines"),
            origin =
                Airport(
                    id = 1,
                    iataCode = "GRU",
                    name = "Guarulhos",
                    city = "São Paulo",
                    country = "Brasil",
                    photoUrl = "https://example.com/photo.jpg",
                    region = "América do Sul",
                    isPopular = false,
                ),
            destination =
                Airport(
                    id = 2,
                    iataCode = "GIG",
                    name = "Galeão",
                    city = "Rio de Janeiro",
                    country = "Brasil",
                    photoUrl = "https://example.com/photo.jpg",
                    region = "América do Sul",
                    isPopular = false,
                ),
            departureTime = LocalDateTime.of(2027, 3, 1, 8, 0),
            arrivalTime = LocalDateTime.of(2027, 3, 1, 9, 10),
            seatClass = SeatClass.ECONOMY,
            aircraftType = "Airbus A320",
        )
    protected val flightRepository = ActiveOnlyFlightRepository(listOf(flight))
    protected val logRepository = FakeAiSuggestionLogRepository()

    protected fun useCase(aiService: AiSuggestionService) =
        SuggestFlightsUseCase(flightRepository, aiService, logRepository)
}
