package com.dbook.presentation.dashboard

import kotlin.test.Test
import kotlin.test.assertEquals

class TheTimeSeriesCanBeWeeklyAndOfBookingsOrCustomersTest : DashboardFixture() {
    @Test
    fun `given bookings and customers when asking per week then they land on the Monday of their week`() {
        val monday = newWindow().let { it.plusDays(((8 - it.dayOfWeek.value) % 7).toLong()) }
        val customer = customerCreatedAt(at(monday))
        customerCreatedAt(at(monday.plusDays(9)))
        val flight = flightOn(monday.atTime(8, 0))
        booking(customer, flight, "10.00", at(monday.plusDays(2)))
        booking(customer, flight, "10.00", at(monday.plusDays(3)))
        booking(customer, flight, "10.00", at(monday.plusDays(8)))
        val token = staff()
        val to = monday.plusDays(13)

        val bookings =
            json(get(token, "/v1/admin/dashboard/timeseries?metric=BOOKINGS&granularity=WEEK&from=$monday&to=$to"))
        val customers =
            json(get(token, "/v1/admin/dashboard/timeseries?metric=NEW_CUSTOMERS&granularity=WEEK&from=$monday&to=$to"))

        assertEquals(
            listOf(monday.toString(), monday.plusDays(7).toString()),
            bookings["points"].map { it["date"].asText() },
        )
        assertEquals(listOf(2, 1), bookings["points"].map { it["value"].asInt() })
        assertEquals(listOf(1, 1), customers["points"].map { it["value"].asInt() })
    }
}
