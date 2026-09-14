package com.dbook.application.registerflightusecase

import kotlin.test.Test
import kotlin.test.assertEquals

class GeneratesASeatMapRespectingTheAircraftTypeTest : RegisterFlightUseCaseFixture() {
    @Test
    fun `given an Embraer E195 (2 plus 2) when registering then rows have 4 seats each`() {
        useCase.execute(command(aircraftType = "Embraer E195", totalCapacity = 8))

        val labels = seatRepository.saved.map { it.label }
        assertEquals(listOf("1A", "1B", "1C", "1D", "2A", "2B", "2C", "2D"), labels)
    }

    @Test
    fun `given a Boeing 777 (3 plus 4 plus 3) when registering then rows have 10 seats each`() {
        useCase.execute(command(aircraftType = "Boeing 777", totalCapacity = 10))

        val labels = seatRepository.saved.map { it.label }
        assertEquals(
            listOf("1A", "1B", "1C", "1D", "1E", "1F", "1G", "1H", "1I", "1J"),
            labels,
        )
    }
}
