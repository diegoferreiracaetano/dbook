package com.dbook.application.flight.getlowestpricefordestinationusecase

import com.dbook.application.flight.GetLowestPriceForDestinationUseCase
import java.time.Clock.systemDefaultZone
import kotlin.test.Test
import kotlin.test.assertNull

class ReturnsNullWhenNoFlightsExistTest {
    @Test
    fun `given no flights to a destination when executed then returns null`() {
        val useCase = GetLowestPriceForDestinationUseCase(FakeFlightRepository(null), systemDefaultZone())

        assertNull(useCase.execute("XXX"))
    }
}
