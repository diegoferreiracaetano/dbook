package com.dbook.presentation

import com.dbook.application.RegisterFlightCommand
import com.dbook.application.RegisterFlightUseCase
import com.dbook.domain.Airport
import com.dbook.domain.AirportNotFoundException
import com.dbook.domain.Flight
import com.dbook.domain.SeatClass
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.willThrow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.math.BigDecimal
import java.time.LocalDateTime

@WebMvcTest(FlightAdminController::class)
class FlightAdminControllerTest {

	@Autowired
	lateinit var mockMvc: MockMvc

	@MockBean
	lateinit var registerFlightUseCase: RegisterFlightUseCase

	private val objectMapper = ObjectMapper().registerModule(JavaTimeModule())

	private val gru = Airport(id = 1, iataCode = "GRU", name = "Guarulhos", city = "São Paulo", country = "Brasil")
	private val gig = Airport(id = 2, iataCode = "GIG", name = "Galeão", city = "Rio de Janeiro", country = "Brasil")

	private val request = RegisterFlightRequest(
		flightNumber = "DB1234",
		originIataCode = "GRU",
		destinationIataCode = "GIG",
		departureTime = LocalDateTime.of(2026, 10, 1, 8, 0),
		arrivalTime = LocalDateTime.of(2026, 10, 1, 9, 10),
		seatClass = SeatClass.ECONOMY,
		price = BigDecimal("500.00"),
		totalCapacity = 180,
	)

	private val expectedCommand = RegisterFlightCommand(
		flightNumber = request.flightNumber,
		originIataCode = request.originIataCode,
		destinationIataCode = request.destinationIataCode,
		departureTime = request.departureTime,
		arrivalTime = request.arrivalTime,
		seatClass = request.seatClass,
		price = request.price,
		totalCapacity = request.totalCapacity,
	)

	@Test
	fun `returns 201 with the created flight`() {
		val flight = Flight(
			id = 1,
			title = "DB1234 GRU-GIG",
			price = BigDecimal("500.00"),
			totalCapacity = 180,
			availableCapacity = 180,
			flightNumber = "DB1234",
			origin = gru,
			destination = gig,
			departureTime = request.departureTime,
			arrivalTime = request.arrivalTime,
			seatClass = SeatClass.ECONOMY,
		)
		given(registerFlightUseCase.execute(expectedCommand)).willReturn(flight)

		mockMvc.post("/admin/flights") {
			contentType = MediaType.APPLICATION_JSON
			content = objectMapper.writeValueAsString(request)
		}.andExpect {
			status { isCreated() }
			jsonPath("$.flightNumber") { value("DB1234") }
			jsonPath("$.origin") { value("GRU") }
			jsonPath("$.destination") { value("GIG") }
		}
	}

	@Test
	fun `returns 404 when an airport does not exist`() {
		given(registerFlightUseCase.execute(expectedCommand))
			.willThrow(AirportNotFoundException("GRU"))

		mockMvc.post("/admin/flights") {
			contentType = MediaType.APPLICATION_JSON
			content = objectMapper.writeValueAsString(request)
		}.andExpect {
			status { isNotFound() }
		}
	}
}
