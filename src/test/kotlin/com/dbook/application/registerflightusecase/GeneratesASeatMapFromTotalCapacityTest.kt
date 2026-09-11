package com.dbook.application.registerflightusecase

import kotlin.test.Test
import kotlin.test.assertEquals

class GeneratesASeatMapFromTotalCapacityTest : RegisterFlightUseCaseFixture() {
    @Test
    fun `given a totalCapacity of 180 when registering a flight then it generates 180 seats labeled 1A through 30F`() {
        useCase.execute(command())

        assertEquals(180, seatRepository.saved.size)
        assertEquals("1A", seatRepository.saved.first().label)
        assertEquals("30F", seatRepository.saved.last().label)
    }
}
