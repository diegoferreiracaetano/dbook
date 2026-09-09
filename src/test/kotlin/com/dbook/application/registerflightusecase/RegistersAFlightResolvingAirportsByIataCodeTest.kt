package com.dbook.application.registerflightusecase

import kotlin.test.Test
import kotlin.test.assertEquals

class RegistersAFlightResolvingAirportsByIataCodeTest : RegisterFlightUseCaseFixture() {
    @Test
    fun `given valid GRU and GIG codes when registering then airports resolve and the flight is saved`() {
        val flight = useCase.execute(command())

        assertEquals("GRU", flight.origin.iataCode)
        assertEquals("GIG", flight.destination.iataCode)
        assertEquals(180, flight.availableCapacity)
        assertEquals(1, flightRepository.saved.size)
    }
}
