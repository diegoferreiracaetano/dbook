package com.dbook.presentation.flightsearchcontroller

import com.dbook.application.SearchFlightsUseCase
import com.dbook.domain.Airport
import com.dbook.domain.TokenService
import com.dbook.presentation.FlightSearchController
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

    // JwtAuthenticationFilter is still instantiated as a bean even with addFilters =
    // false (which only skips invoking it during dispatch), so its TokenService
    // dependency still needs to be satisfiable.
    @MockBean
    lateinit var tokenService: TokenService

    protected val gru = Airport(id = 1, iataCode = "GRU", name = "Guarulhos", city = "São Paulo", country = "Brasil")
    protected val gig = Airport(id = 2, iataCode = "GIG", name = "Galeão", city = "Rio de Janeiro", country = "Brasil")
}
