package com.dbook.presentation

import com.dbook.application.SearchFlightsUseCase
import com.dbook.domain.Airport
import com.dbook.domain.Flight
import com.dbook.domain.SeatClass
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@WebMvcTest(FlightSearchController::class)
class FlightSearchControllerTest {

	@Autowired
	lateinit var mockMvc: MockMvc

	@MockBean
	lateinit var searchFlightsUseCase: SearchFlightsUseCase

	private val gru = Airport(id = 1, iataCode = "GRU", name = "Guarulhos", city = "São Paulo", country = "Brasil")
	private val gig = Airport(id = 2, iataCode = "GIG", name = "Galeão", city = "Rio de Janeiro", country = "Brasil")

	@Test
	fun `returns matching flights`() {
		val flight = Flight(
			id = 1,
			title = "DB1234 GRU-GIG",
			price = BigDecimal("500.00"),
			totalCapacity = 180,
			availableCapacity = 180,
			flightNumber = "DB1234",
			origin = gru,
			destination = gig,
			departureTime = LocalDateTime.of(2026, 10, 1, 8, 0),
			arrivalTime = LocalDateTime.of(2026, 10, 1, 9, 10),
			seatClass = SeatClass.ECONOMY,
		)
		given(searchFlightsUseCase.execute("GRU", "GIG", LocalDate.of(2026, 10, 1)))
			.willReturn(listOf(flight))

		mockMvc.get("/flights/search") {
			param("origin", "GRU")
			param("destination", "GIG")
			param("date", "2026-10-01")
		}.andExpect {
			status { isOk() }
			jsonPath("$[0].flightNumber") { value("DB1234") }
		}
	}

	@Test
	fun `returns empty list when nothing matches`() {
		given(searchFlightsUseCase.execute("GRU", "JFK", LocalDate.of(2026, 10, 1)))
			.willReturn(emptyList())

		mockMvc.get("/flights/search") {
			param("origin", "GRU")
			param("destination", "JFK")
			param("date", "2026-10-01")
		}.andExpect {
			status { isOk() }
			jsonPath("$") { isEmpty() }
		}
	}
}
