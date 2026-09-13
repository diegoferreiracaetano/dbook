package com.dbook.application.getlowestpricefordestinationusecase

import com.dbook.application.GetLowestPriceForDestinationUseCase
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class ReturnsTheLowestPriceOverA60DayWindowTest {
    @Test
    fun `given flights to a destination when executed then returns the lowest price over a 60-day window`() {
        val repository = FakeFlightRepository(BigDecimal("450.00"))
        val useCase = GetLowestPriceForDestinationUseCase(repository)

        val price = useCase.execute("GIG")

        assertEquals(BigDecimal("450.00"), price)
        assertEquals("GIG", repository.lastDestination)
        val today = LocalDate.now()
        assertEquals(today, repository.lastFrom)
        assertEquals(today.plusDays(LOOKAHEAD_DAYS), repository.lastTo)
    }
}
