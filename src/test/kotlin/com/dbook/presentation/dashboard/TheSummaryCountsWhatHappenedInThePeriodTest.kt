package com.dbook.presentation.dashboard

import kotlin.test.Test
import kotlin.test.assertEquals

class TheSummaryCountsWhatHappenedInThePeriodTest : DashboardFixture() {
    @Test
    fun `given paid, refunded, pending and expired bookings when summarizing then the numbers follow the glossary`() {
        val start = newWindow()
        val customer = customerCreatedAt(at(start.plusDays(1)))
        customerCreatedAt(at(start.plusDays(2)))
        customerCreatedAt(at(start.minusDays(30)))
        val flight = flightOn(start.plusDays(3).atTime(8, 0), capacity = 12)
        reserveSeats(flight, 3)
        val outsideFlight = flightOn(start.plusDays(40).atTime(8, 0), capacity = 12)
        reserveSeats(outsideFlight, 12)

        booking(
            customer,
            flight,
            "100.00",
            at(start.plusDays(1)),
            Triple("CONFIRMED", at(start.plusDays(2)), customer),
        )
        val b =
            booking(
                customer,
                flight,
                "200.00",
                at(start.plusDays(3)),
                Triple("CONFIRMED", at(start.plusDays(4)), customer),
                Triple("REFUNDED", at(start.plusDays(7)), 1L),
            )
        booking(customer, flight, "300.00", at(start.plusDays(1)))
        booking(
            customer,
            flight,
            "400.00",
            at(start.plusDays(3)),
            Triple("CANCELLED", at(start.plusDays(3), "12:15:00"), null),
        )
        // created before the period but paid inside it: it is revenue of the period, not a booking of it
        booking(customer, flight, "50.00", at(start.minusDays(2)), Triple("CONFIRMED", at(start.plusDays(1)), customer))
        refundCompletedAt(b, customer, "200.00", at(start.plusDays(7)))

        val result = summary(staff(), start, start.plusDays(9))

        assertEquals(350.0, result["grossRevenue"].asDouble())
        assertEquals(200.0, result["refunded"].asDouble())
        assertEquals(150.0, result["netRevenue"].asDouble())
        assertEquals(2, result["newCustomers"].asInt())
        assertEquals(0.5, result["conversionRate"].asDouble())
        assertEquals(0.25, result["expirationRate"].asDouble())
        assertEquals(0.25, result["averageOccupancy"].asDouble())
        val byStatus = result["bookingsByStatus"]
        assertEquals(
            listOf("CANCELLED", "CONFIRMED", "PENDING", "REFUNDED"),
            byStatus.fieldNames().asSequence().toList(),
        )
        assertEquals(1, byStatus["CONFIRMED"].asInt())
    }
}
