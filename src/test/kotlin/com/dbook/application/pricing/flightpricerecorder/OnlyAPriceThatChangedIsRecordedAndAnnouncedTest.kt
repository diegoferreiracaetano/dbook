package com.dbook.application.pricing.flightpricerecorder

import com.dbook.application.pricing.FlightPriceRecorderFixture
import com.dbook.domain.flight.FlightEvents
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OnlyAPriceThatChangedIsRecordedAndAnnouncedTest : FlightPriceRecorderFixture() {
    @Test
    fun `given a changed price when recorded then it joins the history and an event carries old and new`() {
        recorder.record(flightPricedAt("250.00"), previous = BigDecimal("300.00"))

        assertEquals(listOf(BigDecimal("250.00")), history.of(5, 10).map { it.price })
        val event = outbox.events.single()
        assertEquals(FlightEvents.PRICE_CHANGED, event.type)
        assertEquals("250.00", event.payload["price"])
        assertEquals("300.00", event.payload["previousPrice"])
        assertEquals(now, event.availableAt)
    }

    @Test
    fun `given the same price when recorded then nothing is written, even with another number of decimals`() {
        recorder.record(flightPricedAt("300.00"), previous = BigDecimal("300.00"))
        recorder.record(flightPricedAt("300.00"), previous = BigDecimal("300"))

        assertTrue(history.rows.isEmpty())
        assertTrue(outbox.events.isEmpty())
    }
}
