package com.dbook.application.getlowestpricefordestinationusecase

import com.dbook.application.GetLowestPriceForDestinationUseCase
import kotlin.test.Test
import kotlin.test.assertNull

class ReturnsNullWhenNoFlightsExistTest {
    @Test
    fun `given no flights to a destination when executed then returns null`() {
        val useCase = GetLowestPriceForDestinationUseCase(FakeFlightRepository(null))

        assertNull(useCase.execute("XXX"))
    }
}
