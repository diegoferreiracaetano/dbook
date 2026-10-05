package com.dbook.domain.payment.refund

import com.dbook.domain.payment.RefundPolicy
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class TheWindowClosesTwentyFourHoursBeforeDepartureTest {
    private val departure = LocalDateTime.of(2026, 10, 10, 12, 0)

    @Test
    fun `given the moment exactly 24 hours before then no override is needed, one second later it is`() {
        assertEquals(false, RefundPolicy.needsOverride(departure, departure.minusHours(24)))
        assertEquals(true, RefundPolicy.needsOverride(departure, departure.minusHours(24).plusSeconds(1)))
    }

    @Test
    fun `given a departure already past or no departure at all then past needs an override and none never does`() {
        assertEquals(true, RefundPolicy.needsOverride(departure, departure.plusHours(1)))
        assertEquals(false, RefundPolicy.needsOverride(null, departure))
    }
}
