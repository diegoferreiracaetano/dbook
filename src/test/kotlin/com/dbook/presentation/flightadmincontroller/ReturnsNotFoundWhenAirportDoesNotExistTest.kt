package com.dbook.presentation.flightadmincontroller

import com.dbook.domain.AirportNotFoundException
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.post

class ReturnsNotFoundWhenAirportDoesNotExistTest : FlightAdminControllerFixture() {
    @Test
    fun `given a nonexistent airport when posting to admin flights then it returns 404`() {
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
