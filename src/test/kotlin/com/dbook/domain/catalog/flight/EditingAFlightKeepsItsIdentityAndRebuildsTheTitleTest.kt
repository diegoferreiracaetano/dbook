package com.dbook.domain.catalog.flight

import com.dbook.domain.catalog.FlightEdit
import com.dbook.domain.catalog.SeatClass
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals

class EditingAFlightKeepsItsIdentityAndRebuildsTheTitleTest {
    @Test
    fun `given a flight when edited then the id stays, the title follows the route and the capacity is the new one`() {
        val edited =
            aFlight().edit(
                FlightEdit(
                    "DB9", airline, gig, gru, departure.plusDays(1), departure.plusDays(1).plusHours(2),
                    SeatClass.BUSINESS, BigDecimal("250.00"), 30, "Boeing 777",
                ),
            )

        assertEquals(5L, edited.id)
        assertEquals("DB9 GIG-GRU", edited.title)
        assertEquals(30, edited.totalCapacity)
        assertEquals(BigDecimal("250.00"), edited.price)
    }
}
