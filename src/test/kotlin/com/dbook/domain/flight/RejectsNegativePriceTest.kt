package com.dbook.domain.flight

import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertFailsWith

class RejectsNegativePriceTest : FlightTestFixture() {
    @Test
    fun `given a negative price when a Flight is built then it throws IllegalArgumentException`() {
        assertFailsWith<IllegalArgumentException> {
            buildFlight(price = BigDecimal("-1.00"))
        }
    }
}
