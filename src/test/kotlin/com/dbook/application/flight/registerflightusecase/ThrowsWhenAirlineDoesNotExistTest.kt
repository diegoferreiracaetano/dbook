package com.dbook.application.flight.registerflightusecase

import com.dbook.domain.flight.AirlineNotFoundException
import kotlin.test.Test
import kotlin.test.assertFailsWith

class ThrowsWhenAirlineDoesNotExistTest : RegisterFlightUseCaseFixture() {
    @Test
    fun `given a nonexistent airline code when registering then it throws AirlineNotFoundException`() {
        assertFailsWith<AirlineNotFoundException> {
            useCase.execute(command(airline = "XX"))
        }
    }
}
