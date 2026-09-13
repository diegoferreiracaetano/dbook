package com.dbook.presentation.flightsearchcontroller

import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.test.web.servlet.get
import java.math.BigDecimal

class ReturnsTheLowestPriceForADestinationTest : FlightSearchControllerFixture() {
    @Test
    fun `given a destination with flights when getting the lowest price then it returns 200 with the price`() {
        given(getLowestPriceForDestinationUseCase.execute("GIG")).willReturn(BigDecimal("450.00"))

        mockMvc.get("/flights/lowest-price") {
            param("destination", "GIG")
        }.andExpect {
            status { isOk() }
            jsonPath("$.destination") { value("GIG") }
            jsonPath("$.lowestPrice") { value(450.00) }
        }
    }
}
