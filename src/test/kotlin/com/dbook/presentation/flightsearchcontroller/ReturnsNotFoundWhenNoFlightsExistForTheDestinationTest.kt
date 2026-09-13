package com.dbook.presentation.flightsearchcontroller

import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.test.web.servlet.get

class ReturnsNotFoundWhenNoFlightsExistForTheDestinationTest : FlightSearchControllerFixture() {
    @Test
    fun `given a destination with no flights when getting the lowest price then it returns 404`() {
        given(getLowestPriceForDestinationUseCase.execute("XXX")).willReturn(null)

        mockMvc.get("/flights/lowest-price") {
            param("destination", "XXX")
        }.andExpect {
            status { isNotFound() }
        }
    }
}
