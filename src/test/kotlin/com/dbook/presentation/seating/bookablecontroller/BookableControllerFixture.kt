package com.dbook.presentation.seating.bookablecontroller

import com.dbook.application.seating.GetSeatMapUseCase
import com.dbook.domain.identity.TokenService
import com.dbook.domain.identity.UserRepository
import com.dbook.presentation.seating.BookableController
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.web.servlet.MockMvc

// This slice tests HTTP/business behavior only — security filters are disabled on
// purpose (addFilters = false), since @WebMvcTest doesn't load the real SecurityConfig
// anyway. See FlightSearchControllerFixture for why tokenService is still needed.
@WebMvcTest(BookableController::class)
@AutoConfigureMockMvc(addFilters = false)
abstract class BookableControllerFixture {
    @Autowired
    lateinit var mockMvc: MockMvc

    @MockBean
    lateinit var getSeatMapUseCase: GetSeatMapUseCase

    @MockBean
    lateinit var tokenService: TokenService

    @MockBean
    lateinit var userRepository: UserRepository
}
