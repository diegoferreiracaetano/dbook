package com.dbook.presentation.flightadmincontroller

import com.dbook.application.RegisterFlightCommand
import com.dbook.application.RegisterFlightUseCase
import com.dbook.domain.Airport
import com.dbook.domain.SeatClass
import com.dbook.domain.TokenService
import com.dbook.presentation.FlightAdminController
import com.dbook.presentation.RegisterFlightRequest
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.web.servlet.MockMvc
import java.math.BigDecimal
import java.time.LocalDateTime

// This slice tests HTTP/business behavior only — security filters are disabled on
// purpose (addFilters = false), since @WebMvcTest doesn't load the real SecurityConfig
// anyway. The @PreAuthorize("hasRole('ADMIN')") gate and the unauthenticated/wrong-role
// cases (item 3.8) are tested in SecurityIntegrationTest against the full application
// context, where the real filter chain and method-security config are active.
@WebMvcTest(FlightAdminController::class)
@AutoConfigureMockMvc(addFilters = false)
abstract class FlightAdminControllerFixture {
    @Autowired
    lateinit var mockMvc: MockMvc

    @MockBean
    lateinit var registerFlightUseCase: RegisterFlightUseCase

    // see FlightSearchControllerFixture for why this is still needed despite addFilters = false
    @MockBean
    lateinit var tokenService: TokenService

    protected val objectMapper: ObjectMapper = ObjectMapper().registerModule(JavaTimeModule())

    protected val gru = Airport(id = 1, iataCode = "GRU", name = "Guarulhos", city = "São Paulo", country = "Brasil")
    protected val gig = Airport(id = 2, iataCode = "GIG", name = "Galeão", city = "Rio de Janeiro", country = "Brasil")

    protected val request =
        RegisterFlightRequest(
            flightNumber = "DB1234",
            originIataCode = "GRU",
            destinationIataCode = "GIG",
            departureTime = LocalDateTime.of(2026, 10, 1, 8, 0),
            arrivalTime = LocalDateTime.of(2026, 10, 1, 9, 10),
            seatClass = SeatClass.ECONOMY,
            price = BigDecimal("500.00"),
            totalCapacity = 180,
        )

    protected val expectedCommand =
        RegisterFlightCommand(
            flightNumber = request.flightNumber,
            originIataCode = request.originIataCode,
            destinationIataCode = request.destinationIataCode,
            departureTime = request.departureTime,
            arrivalTime = request.arrivalTime,
            seatClass = request.seatClass,
            price = request.price,
            totalCapacity = request.totalCapacity,
        )
}
