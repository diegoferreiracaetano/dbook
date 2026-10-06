package com.dbook.presentation.flight.flightsearchcontroller

import com.dbook.application.flight.GetLowestPriceForDestinationUseCase
import com.dbook.application.flight.SearchFlightsUseCase
import com.dbook.domain.catalog.Airport
import com.dbook.domain.flight.Airline
import com.dbook.domain.identity.TokenService
import com.dbook.domain.identity.UserRepository
import com.dbook.infrastructure.web.FixedWindowCounter
import com.dbook.presentation.flight.FlightSearchController
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.web.servlet.MockMvc

// This slice tests HTTP/business behavior only — security filters are disabled on
// purpose (addFilters = false), since @WebMvcTest doesn't load the real SecurityConfig
// anyway. Real security enforcement (public vs protected, roles, tokens) is tested in
// SecurityIntegrationTest against the full application context.
@WebMvcTest(FlightSearchController::class)
@AutoConfigureMockMvc(addFilters = false)
abstract class FlightSearchControllerFixture {
    @Autowired
    lateinit var mockMvc: MockMvc

    @MockBean
    lateinit var searchFlightsUseCase: SearchFlightsUseCase

    @MockBean
    lateinit var getLowestPriceForDestinationUseCase: GetLowestPriceForDestinationUseCase

    // JwtAuthenticationFilter is still instantiated as a bean even with addFilters =
    // false (which only skips invoking it during dispatch), so its TokenService
    // dependency still needs to be satisfiable.
    @MockBean
    lateinit var tokenService: TokenService

    @MockBean
    lateinit var userRepository: UserRepository

    // the request limits sit in front of every controller; the slice has no Redis to count in
    @MockBean
    lateinit var requestCounter: FixedWindowCounter

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
}
