package com.dbook.domain.pricing

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertFailsWith

class APriceAlertKeepsItsInvariantsTest {
    private fun alert(
        origin: String = "GRU",
        destination: String = "GIG",
        target: String = "300.00",
    ) = PriceAlert(
        userId = 1,
        origin = origin,
        destination = destination,
        travelDate = LocalDate.of(2027, 1, 15),
        targetPrice = BigDecimal(target),
        createdAt = Instant.EPOCH,
    )

    @Test
    fun `given bad data when an alert is created then it is refused`() {
        assertFailsWith<IllegalArgumentException> { alert(origin = "gru") }
        assertFailsWith<IllegalArgumentException> { alert(destination = "GIGA") }
        assertFailsWith<IllegalArgumentException> { alert(destination = "GRU") }
        assertFailsWith<IllegalArgumentException> { alert(target = "0") }
        assertFailsWith<IllegalArgumentException> { alert(target = "-5") }
        assertFailsWith<IllegalArgumentException> { alert(target = "1000001") }
    }
}
