package com.dbook.presentation.flightsearchcontroller

import com.dbook.domain.Flight
import com.dbook.domain.SeatClass
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.test.web.servlet.get
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

class ReturnsMatchingFlightsTest : FlightSearchControllerFixture() {
    @Test
    fun `given a matching route and date when searching then it returns the flight`() {
        val flight =
            Flight(
                id = 1,
                title = "DB1234 GRU-GIG",
                price = BigDecimal("500.00"),
                totalCapacity = 180,
                availableCapacity = 180,
                flightNumber = "DB1234",
                origin = gru,
                destination = gig,
                departureTime = LocalDateTime.of(2026, 10, 1, 8, 0),
                arrivalTime = LocalDateTime.of(2026, 10, 1, 9, 10),
                seatClass = SeatClass.ECONOMY,
            )
        given(searchFlightsUseCase.execute("GRU", "GIG", LocalDate.of(2026, 10, 1)))
            .willReturn(listOf(flight))

        mockMvc.get("/flights/search") {
            param("origin", "GRU")
            param("destination", "GIG")
            param("date", "2026-10-01")
        }.andExpect {
            status { isOk() }
            jsonPath("$[0].flightNumber") { value("DB1234") }
        }
    }
}
