package com.dbook.infrastructure.ai.localaisuggestionservice

import com.dbook.domain.catalog.Airport
import com.dbook.domain.flight.Airline
import com.dbook.domain.flight.Flight
import com.dbook.domain.flight.SeatClass
import com.dbook.infrastructure.ai.LocalAiSuggestionService
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TheLocalSuggestionsFollowTheRequestAndOnlyOfferWhatIsAvailableTest {
    private val service = LocalAiSuggestionService()
    private val latam = Airline(1, "LA", "LATAM")
    private val gru = Airport(1, "GRU", "Guarulhos", "São Paulo", "Brasil", "https://x/a.jpg", "América do Sul", true)
    private val gig = Airport(2, "GIG", "Galeão", "Rio de Janeiro", "Brasil", "https://x/b.jpg", "América do Sul", true)
    private val lis = Airport(3, "LIS", "Humberto Delgado", "Lisboa", "Portugal", "https://x/c.jpg", "Europa", true)

    private fun flight(
        id: Long,
        destination: Airport,
        price: String,
        seats: Int = 10,
        seatClass: SeatClass = SeatClass.ECONOMY,
    ) = Flight(
        id = id, title = "DB$id",
        price =
            BigDecimal(
                price,
            ),
        totalCapacity = 12, availableCapacity = seats, active = true,
        flightNumber = "DB$id", airline = latam, origin = gru, destination = destination,
        departureTime =
            LocalDateTime.of(
                2027,
                2,
                1,
                8,
                0,
            ).plusHours(id),
        arrivalTime = LocalDateTime.of(2027, 2, 1, 12, 0),
        seatClass = seatClass, aircraftType = "Airbus A320",
    )

    @Test
    fun `given a city in the request when suggesting then its flights come first, cheapest first on a cheap ask`() {
        val flights = listOf(flight(1, lis, "900"), flight(2, gig, "500"), flight(3, gig, "300"))

        val result = service.suggest("quero um voo barato para o Rio de Janeiro", flights)

        assertEquals(listOf(3L, 2L), result.suggestions.take(2).map { it.flightId })
        assertTrue(result.suggestions.first().reason.contains("Rio de Janeiro"))
    }

    @Test
    fun `given a flight with no seat left when suggesting then it is never offered`() {
        val flights = listOf(flight(1, gig, "100", seats = 0), flight(2, gig, "200"))

        val result = service.suggest("rio", flights)

        assertEquals(listOf(2L), result.suggestions.map { it.flightId })
    }

    @Test
    fun `given a request that names nothing known when suggesting then it offers the cheapest and says so`() {
        val flights = listOf(flight(1, lis, "900"), flight(2, gig, "300"))

        val result = service.suggest("qualquer coisa", flights)

        assertEquals(2L, result.suggestions.first().flightId)
        assertTrue(result.suggestions.first().reason.startsWith("Não achei o destino"))
    }

    @Test
    fun `given the cabin class in the request when suggesting then it prefers that class`() {
        val flights = listOf(flight(1, gig, "300"), flight(2, gig, "800", seatClass = SeatClass.BUSINESS))

        val result = service.suggest("rio em classe executiva", flights)

        assertEquals(2L, result.suggestions.first().flightId)
    }
}
