package com.dbook.presentation.flightsearchcontroller

import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.test.web.servlet.get
import java.time.LocalDate

class ReturnsEmptyListWhenNothingMatchesTest : FlightSearchControllerFixture() {
    @Test
    fun `given no matching flights when searching then it returns an empty list`() {
        given(searchFlightsUseCase.execute("GRU", "JFK", LocalDate.of(2026, 10, 1)))
            .willReturn(emptyList())

        mockMvc.get("/flights/search") {
            param("origin", "GRU")
            param("destination", "JFK")
            param("date", "2026-10-01")
        }.andExpect {
            status { isOk() }
            jsonPath("$") { isEmpty() }
        }
    }
}
