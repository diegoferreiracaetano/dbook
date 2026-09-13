package com.dbook.presentation.destinationcontroller

import com.dbook.application.GetFeaturedDestinationsUseCase
import com.dbook.domain.TokenService
import com.dbook.presentation.DestinationController
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.web.servlet.MockMvc

// This slice tests HTTP/business behavior only — security filters are disabled on
// purpose (addFilters = false), since @WebMvcTest doesn't load the real SecurityConfig
// anyway. Real security enforcement (public vs protected) is tested in
// SecurityIntegrationTest against the full application context.
@WebMvcTest(DestinationController::class)
@AutoConfigureMockMvc(addFilters = false)
abstract class DestinationControllerFixture {
    @Autowired
    lateinit var mockMvc: MockMvc

    @MockBean
    lateinit var getFeaturedDestinationsUseCase: GetFeaturedDestinationsUseCase

    // JwtAuthenticationFilter is still instantiated as a bean even with addFilters =
    // false (which only skips invoking it during dispatch), so its TokenService
    // dependency still needs to be satisfiable.
    @MockBean
    lateinit var tokenService: TokenService
}
