package com.dbook.presentation.flight.flightadmincontroller

import com.dbook.application.flight.CancelFlightUseCase
import com.dbook.application.flight.GetAdminFlightUseCase
import com.dbook.application.flight.RegisterFlightCommand
import com.dbook.application.flight.RegisterFlightUseCase
import com.dbook.application.flight.SearchAdminFlightsUseCase
import com.dbook.application.flight.UpdateFlightUseCase
import com.dbook.domain.catalog.Airport
import com.dbook.domain.flight.Airline
import com.dbook.domain.flight.SeatClass
import com.dbook.domain.identity.Actor
import com.dbook.domain.identity.Role
import com.dbook.domain.identity.TokenService
import com.dbook.domain.identity.UserRepository
import com.dbook.infrastructure.web.FixedWindowCounter
import com.dbook.presentation.flight.FlightAdminController
import com.dbook.presentation.flight.RegisterFlightRequest
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
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

    @MockBean
    lateinit var searchAdminFlightsUseCase: SearchAdminFlightsUseCase

    @MockBean
    lateinit var getAdminFlightUseCase: GetAdminFlightUseCase

    @MockBean
    lateinit var updateFlightUseCase: UpdateFlightUseCase

    @MockBean
    lateinit var cancelFlightUseCase: CancelFlightUseCase

    // see FlightSearchControllerFixture for why this is still needed despite addFilters = false
    @MockBean
    lateinit var tokenService: TokenService

    @MockBean
    lateinit var userRepository: UserRepository

    // the request limits sit in front of every controller; the slice has no Redis to count in
    @MockBean
    lateinit var requestCounter: FixedWindowCounter

    protected val objectMapper: ObjectMapper = ObjectMapper().registerModule(JavaTimeModule())

    // the caller handed to the controller directly, instead of through a JWT (see PaymentControllerFixture)
    protected val authentication =
        UsernamePasswordAuthenticationToken("1", null, listOf(SimpleGrantedAuthority("ROLE_SUPER_ADMIN")))

    protected val gru =
        Airport(
            id = 1,
            iataCode = "GRU",
            name = "Guarulhos",
            city = "São Paulo",
            country = "Brasil",
            photoUrl = "https://example.com/photo.jpg",
            region = "América do Sul",
            isPopular = false,
        )
    protected val gig =
        Airport(
            id = 2,
            iataCode = "GIG",
            name = "Galeão",
            city = "Rio de Janeiro",
            country = "Brasil",
            photoUrl = "https://example.com/photo.jpg",
            region = "América do Sul",
            isPopular = false,
        )
    protected val latam = Airline(id = 1, iataCode = "LA", name = "LATAM Airlines")

    protected val request =
        RegisterFlightRequest(
            flightNumber = "DB1234",
            airlineIataCode = "LA",
            originIataCode = "GRU",
            destinationIataCode = "GIG",
            departureTime = LocalDateTime.of(2026, 10, 1, 8, 0),
            arrivalTime = LocalDateTime.of(2026, 10, 1, 9, 10),
            seatClass = SeatClass.ECONOMY,
            price = BigDecimal("500.00"),
            totalCapacity = 180,
            aircraftType = "Airbus A320",
        )

    protected val expectedCommand =
        RegisterFlightCommand(
            actor = Actor(id = 1, role = Role.SUPER_ADMIN),
            flightNumber = request.flightNumber,
            airlineIataCode = request.airlineIataCode,
            originIataCode = request.originIataCode,
            destinationIataCode = request.destinationIataCode,
            departureTime = request.departureTime,
            arrivalTime = request.arrivalTime,
            seatClass = request.seatClass,
            price = request.price,
            totalCapacity = request.totalCapacity,
            aircraftType = request.aircraftType,
        )
}
