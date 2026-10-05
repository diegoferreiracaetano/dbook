package com.dbook.domain.catalog.flight

import com.dbook.domain.catalog.FlightEdit
import com.dbook.domain.catalog.FlightStatus
import com.dbook.domain.catalog.SeatClass
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ACancelledFlightCannotBeEditedOrCancelledAgainTest {
    @Test
    fun `given a cancelled flight when editing or cancelling again then it is refused, and the status is derived`() {
        val cancelled = aFlight().cancel()

        assertEquals(FlightStatus.CANCELLED, cancelled.status)
        assertEquals(FlightStatus.SCHEDULED, aFlight().status)
        assertFailsWith<IllegalStateException> { cancelled.cancel() }
        assertFailsWith<IllegalStateException> {
            cancelled.edit(
                FlightEdit(
                    "DB1", airline, gru, gig, departure,
                    departure.plusHours(
                        1,
                    ),
                    SeatClass.ECONOMY, BigDecimal.ONE, 1, "x",
                ),
            )
        }
    }
}
