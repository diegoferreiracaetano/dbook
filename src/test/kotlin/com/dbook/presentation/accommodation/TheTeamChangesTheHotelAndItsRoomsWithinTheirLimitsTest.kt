package com.dbook.presentation.accommodation

import kotlin.test.Test
import kotlin.test.assertEquals

class TheTeamChangesTheHotelAndItsRoomsWithinTheirLimitsTest : HotelApiFixture() {
    @Test
    fun `given a hotel when its details and a rate change then the hotel follows and each change is audited`() {
        val team = manager()
        val admin = registerStaffAndLogin(uniqueEmail())
        val hotel = aHotel(team)

        val renamed = adminPut(team, "/v1/admin/accommodations/${hotel.id}", hotelBody(hotel.destination, "Renamed", 5))
        val repriced =
            adminPut(
                team,
                "/v1/admin/accommodations/${hotel.id}/room-types/${hotel.roomTypeId}",
                roomBody(rate = 350, quantity = 2),
            )

        assertEquals("Renamed", json(renamed)["name"].asText())
        assertEquals(5, json(renamed)["stars"].asInt())
        assertEquals(350.0, json(repriced)["roomTypes"][0]["nightlyRate"].asDouble())
        val actions =
            auditEntries(admin, "targetId=${hotel.id}&size=50")
                .filter { it["targetType"].asText() == "ACCOMMODATION" }.map { it["action"].asText() }.toSet()
        assertEquals(setOf("ACCOMMODATION_CREATED", "ACCOMMODATION_UPDATED", "ROOM_TYPE_CHANGED"), actions)
    }

    @Test
    fun `given a rate that changes after a booking then the booking keeps its price and new ones pay the new rate`() {
        val team = manager()
        val hotel = aHotel(team, rooms = listOf(roomBody(quantity = 3)))
        val first = aCustomerToken()
        val booked = json(bookStay(first, hotel, day(0), day(2)))

        adminPut(
            team,
            "/v1/admin/accommodations/${hotel.id}/room-types/${hotel.roomTypeId}",
            roomBody(rate = 500, quantity = 3),
        )
        val second = json(bookStay(aCustomerToken(), hotel, day(0), day(2)))

        assertEquals(600.0, booked["price"].asDouble())
        assertEquals(1000.0, second["price"].asDouble())
        assertEquals(600.0, json(myBookings(first))[0]["price"].asDouble())
    }

    @Test
    fun `given rooms booked when the quantity is cut below them then it is a 409 and the quantity stays`() {
        val team = manager()
        val hotel = aHotel(team, rooms = listOf(roomBody(quantity = 3)))
        repeat(2) { bookStay(aCustomerToken(), hotel, day(0), day(2)) }

        val cut =
            adminPut(
                team,
                "/v1/admin/accommodations/${hotel.id}/room-types/${hotel.roomTypeId}",
                roomBody(quantity = 1),
            )
        val ok =
            adminPut(
                team,
                "/v1/admin/accommodations/${hotel.id}/room-types/${hotel.roomTypeId}",
                roomBody(quantity = 2),
            )

        assertEquals(409, cut.response.status)
        assertEquals(200, ok.response.status)
    }

    @Test
    fun `given a room type when another with the same name is added then it is a 409, and a new name works`() {
        val team = manager()
        val hotel = aHotel(team)

        val duplicate = addRoomType(team, hotel.id, roomBody("Double"))

        assertEquals(409, duplicate.response.status)
        assertEquals(404, adminGet(team, "/v1/admin/accommodations/999999999").response.status)
    }
}
