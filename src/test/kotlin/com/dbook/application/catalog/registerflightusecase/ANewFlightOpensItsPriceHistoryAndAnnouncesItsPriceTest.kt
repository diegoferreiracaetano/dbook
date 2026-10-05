package com.dbook.application.catalog.registerflightusecase

import com.dbook.domain.catalog.FlightEvents
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ANewFlightOpensItsPriceHistoryAndAnnouncesItsPriceTest : RegisterFlightUseCaseFixture() {
    @Test
    fun `given a new flight when registered then its opening price is the first point and a price event is written`() {
        val flight = useCase.execute(command())

        assertEquals(listOf(BigDecimal("500.00")), priceHistory.of(flight.id!!, 10).map { it.price })
        val event = outbox.events.single { it.type == FlightEvents.PRICE_CHANGED }
        assertEquals("500.00", event.payload["price"])
        assertNull(event.payload["previousPrice"])
        assertEquals("GRU", event.payload["origin"])
        assertEquals("2026-10-01", event.payload["date"])
    }
}
