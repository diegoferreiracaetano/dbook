package com.dbook.presentation.flightadmincontroller

import com.dbook.domain.Flight
import com.dbook.domain.SeatClass
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.post
import java.math.BigDecimal

class ReturnsCreatedWithTheCreatedFlightTest : FlightAdminControllerFixture() {
    @Test
    fun `given a valid request when posting to admin flights then it returns 201 with the created flight`() {
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
                departureTime = request.departureTime,
                arrivalTime = request.arrivalTime,
                seatClass = SeatClass.ECONOMY,
            )
        given(registerFlightUseCase.execute(expectedCommand)).willReturn(flight)

        mockMvc.post("/admin/flights") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isCreated() }
            jsonPath("$.flightNumber") { value("DB1234") }
            jsonPath("$.origin") { value("GRU") }
            jsonPath("$.destination") { value("GIG") }
        }
    }
}
