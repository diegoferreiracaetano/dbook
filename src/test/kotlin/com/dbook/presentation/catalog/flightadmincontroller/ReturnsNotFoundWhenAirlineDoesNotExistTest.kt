package com.dbook.presentation.catalog.flightadmincontroller

import com.dbook.domain.catalog.AirlineNotFoundException
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.post

class ReturnsNotFoundWhenAirlineDoesNotExistTest : FlightAdminControllerFixture() {
    @Test
    fun `given a nonexistent airline when posting to admin flights then it returns 404`() {
        given(registerFlightUseCase.execute(expectedCommand))
            .willThrow(AirlineNotFoundException("XX"))

        mockMvc.post("/admin/flights") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isNotFound() }
            jsonPath("$.error") { value("Airline not found: XX") }
        }
    }
}
