package com.dbook.presentation.accommodation

import org.springframework.http.MediaType
import org.springframework.test.web.servlet.post
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class ABadStayBookingIsRefusedAndNothingIsHeldTest : HotelApiFixture() {
    @Test
    fun `given too many guests, bad or past dates when booking then it is a 400 and no night is held`() {
        val hotel = aHotel()
        val token = aCustomerToken()

        assertEquals(400, bookStay(token, hotel, day(0), day(2), guests = 3).response.status)
        assertEquals(400, bookStay(token, hotel, day(2), day(2)).response.status)
        assertEquals(400, bookStay(token, hotel, LocalDate.now().minusDays(1), day(2)).response.status)
        assertEquals(400, bookStay(token, hotel, day(0), day(40)).response.status)
        assertEquals(0, bookedNightsOf(hotel.roomTypeId))
    }

    @Test
    fun `given an unknown room type, a hotel off sale or nobody signed in when booking then 404, 409 and 401`() {
        val team = manager()
        val hotel = aHotel(team)
        val token = aCustomerToken()

        assertEquals(404, bookRoomType(token, hotel.id, 999_999_999, day(0) to day(2)).response.status)
        assertEquals(401, bookWithoutToken(hotel).response.status)
        adminPost(team, "/v1/admin/accommodations/${hotel.id}/deactivate")
        assertEquals(409, bookStay(token, hotel, day(0), day(2)).response.status)
    }

    @Test
    fun `given a taken night when another guest books an overlapping stay then it is a 409 ROOM_UNAVAILABLE`() {
        val hotel = aHotel()
        bookStay(aCustomerToken(), hotel, day(2), day(4))

        val refused = bookStay(aCustomerToken(), hotel, day(0), day(3))

        assertEquals(409, refused.response.status)
        assertEquals("ROOM_UNAVAILABLE", json(refused)["code"].asText())
        assertEquals(2, bookedNightsOf(hotel.roomTypeId))
    }

    private fun bookWithoutToken(hotel: Hotel) =
        mockMvc.post("/v1/accommodations/${hotel.id}/bookings") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"roomTypeId":${hotel.roomTypeId},"checkIn":"${day(0)}","checkOut":"${day(2)}","guests":2}"""
        }.andReturn()
}
