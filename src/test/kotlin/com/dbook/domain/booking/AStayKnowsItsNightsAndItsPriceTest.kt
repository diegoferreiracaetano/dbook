package com.dbook.domain.booking

import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AStayKnowsItsNightsAndItsPriceTest {
    private fun stay(
        checkIn: String = "2027-01-15",
        checkOut: String = "2027-01-18",
        guests: Int = 2,
        rate: String = "350.00",
    ) = Stay(1, LocalDate.parse(checkIn), LocalDate.parse(checkOut), guests, BigDecimal(rate))

    @Test
    fun `given three nights when asked then the last night is the day before checkout and the total is right`() {
        val stay = stay()

        assertEquals(3, stay.nights)
        assertEquals(listOf("2027-01-15", "2027-01-16", "2027-01-17"), stay.nightDates().map { it.toString() })
        assertEquals(BigDecimal("1050.00"), stay.total)
    }

    @Test
    fun `given a stay across the end of a month when asked then the nights cross it`() {
        assertEquals(
            listOf(
                "2027-01-31",
                "2027-02-01",
            ),
            stay(checkIn = "2027-01-31", checkOut = "2027-02-02").nightDates().map {
                it.toString()
            },
        )
    }

    @Test
    fun `given bad data when a stay is made then it is refused`() {
        assertFailsWith<IllegalArgumentException> { stay(checkOut = "2027-01-15") }
        assertFailsWith<IllegalArgumentException> { stay(checkOut = "2027-01-14") }
        assertFailsWith<IllegalArgumentException> { stay(guests = 0) }
        assertFailsWith<IllegalArgumentException> { stay(rate = "0") }
        assertFailsWith<IllegalArgumentException> { stay(checkOut = "2027-02-16") }
    }
}
