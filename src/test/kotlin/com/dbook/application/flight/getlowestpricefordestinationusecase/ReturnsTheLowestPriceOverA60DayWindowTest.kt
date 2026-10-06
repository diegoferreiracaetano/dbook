package com.dbook.application.flight.getlowestpricefordestinationusecase

import com.dbook.application.flight.GetLowestPriceForDestinationUseCase
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals

class ReturnsTheLowestPriceOverA60DayWindowTest {
    @Test
    fun `given flights to a destination when executed then returns the lowest price over a 60-day window`() {
        val clock = Clock.fixed(Instant.parse("2030-02-15T12:00:00Z"), ZoneOffset.UTC)
        val repository = FakeFlightRepository(BigDecimal("450.00"))
        val useCase = GetLowestPriceForDestinationUseCase(repository, clock)

        val price = useCase.execute("GIG")

        assertEquals(BigDecimal("450.00"), price)
        assertEquals("GIG", repository.lastDestination)
        val today = LocalDate.of(2030, 2, 15)
        assertEquals(today, repository.lastFrom)
        assertEquals(today.plusDays(LOOKAHEAD_DAYS), repository.lastTo)
    }
}
