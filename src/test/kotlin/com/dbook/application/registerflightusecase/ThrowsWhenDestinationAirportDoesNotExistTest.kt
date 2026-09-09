package com.dbook.application.registerflightusecase

import com.dbook.domain.AirportNotFoundException
import kotlin.test.Test
import kotlin.test.assertFailsWith

class ThrowsWhenDestinationAirportDoesNotExistTest : RegisterFlightUseCaseFixture() {
    @Test
    fun `given a nonexistent destination code when registering then it throws AirportNotFoundException`() {
        assertFailsWith<AirportNotFoundException> {
            useCase.execute(command(destination = "YYY"))
        }
    }
}
