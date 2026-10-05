package com.dbook.presentation.accommodation

import com.dbook.application.booking.ExpireBookingUseCase
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.post
import kotlin.test.Test
import kotlin.test.assertEquals

class BookingAStayTakesItsNightsAndEndingItGivesThemBackTest : HotelApiFixture() {
    @Autowired
    lateinit var expireBooking: ExpireBookingUseCase

    @Test
    fun `given a booked stay when searched then its nights are gone, checkout day is free, cancelling returns them`() {
        val hotel = aHotel()
        val token = aCustomerToken()

        val booked = bookStay(token, hotel, day(0), day(3))

        assertEquals(201, booked.response.status)
        assertEquals("PENDING", json(booked)["status"].asText())
        assertEquals(900.0, json(booked)["price"].asDouble())
        assertEquals(3, json(booked)["stay"]["nights"].asInt())
        assertEquals(true, json(booked)["seatId"].isNull)
        assertEquals(emptyList(), found(hotel.destination, day(0), day(3)))
        assertEquals(emptyList(), found(hotel.destination, day(2), day(5)))
        assertEquals(listOf(hotel.id), found(hotel.destination, day(3), day(5)))

        mockMvc.post("/v1/bookings/${json(booked)["id"].asLong()}/cancel") { header("Authorization", "Bearer $token") }
            .andExpect { status { isOk() } }

        assertEquals(listOf(hotel.id), found(hotel.destination, day(0), day(3)))
        assertEquals(0, bookedNightsOf(hotel.roomTypeId))
    }

    @Test
    fun `given a paid stay when the customer lists trips then it shows the stay and the hotel, no flight or seat`() {
        val hotel = aHotel()
        val token = aCustomerToken()
        val booking = json(bookStay(token, hotel, day(0), day(2)))["id"].asLong()
        pay(token, booking)

        val mine = json(myBookings(token)).first { it["id"].asLong() == booking }

        assertEquals("CONFIRMED", mine["status"].asText())
        assertEquals(hotel.id, mine["accommodation"]["id"].asLong())
        assertEquals(day(0).toString(), mine["stay"]["checkIn"].asText())
        assertEquals(true, mine["flight"].isNull)
        assertEquals(true, mine["seat"].isNull)
        assertEquals(600.0, mine["paidAmount"].asDouble())
    }

    @Test
    fun `given a paid stay when the staff refunds it then it is REFUNDED and the nights are free again`() {
        val hotel = aHotel()
        val token = aCustomerToken()
        val booking = json(bookStay(token, hotel, day(0), day(2)))["id"].asLong()
        pay(token, booking)

        val refunded = refund(staff(), booking)

        assertEquals(201, refunded.response.status)
        assertEquals("COMPLETED", bodyOf(refunded)["status"].asText())
        assertEquals(600.0, bodyOf(refunded)["amount"].asDouble())
        assertEquals(0, bookedNightsOf(hotel.roomTypeId))
        assertEquals(listOf(hotel.id), found(hotel.destination, day(0), day(2)))
    }

    @Test
    fun `given a stay left unpaid when its hold runs out then it expires and the nights are free again`() {
        val hotel = aHotel()
        val booking = json(bookStay(aCustomerToken(), hotel, day(0), day(2)))["id"].asLong()
        assertEquals(2, bookedNightsOf(hotel.roomTypeId))

        expireBooking.execute(booking)

        assertEquals(0, bookedNightsOf(hotel.roomTypeId))
        assertEquals("CANCELLED", statusOfBooking(booking))
    }

    @Test
    fun `given a stay when the staff reads the bookings then it is listed with its dates and no seat`() {
        val hotel = aHotel()
        val booking = json(bookStay(aCustomerToken(), hotel, day(1), day(4)))["id"].asLong()

        val row =
            bodyOf(searchBookings(staff(), "page" to "0", "size" to "100")).get("items").first {
                it["id"].asLong() == booking
            }

        assertEquals(true, row["seatLabel"].isNull)
        assertEquals(day(1).toString(), row["checkIn"].asText())
        assertEquals(day(4).toString(), row["checkOut"].asText())
    }

    private fun statusOfBooking(id: Long): String =
        jdbcTemplate.queryForObject("SELECT status FROM booking WHERE id = ?", String::class.java, id).orEmpty()
}
