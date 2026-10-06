package com.dbook.presentation.accommodation

import com.dbook.domain.common.access.Role
import org.springframework.test.web.servlet.get
import kotlin.test.Test
import kotlin.test.assertEquals

class TheTeamCreatesAHotelAndAnyoneCanFindItTest : HotelApiFixture() {
    @Test
    fun `given a hotel when searched for three nights then it shows each free room with the price of the whole stay`() {
        val hotel = aHotel(rooms = listOf(roomBody("Double", 2, 300, 2), roomBody("Suite", 4, 800, 1)))

        val result = json(search(hotel.destination, day(0), day(3), guests = 2))

        assertEquals(1, result["totalElements"].asInt())
        val item = result["items"][0]
        assertEquals(hotel.id, item["id"].asLong())
        assertEquals(900.0, item["fromPrice"].asDouble())
        assertEquals(listOf(900.0, 2400.0), item["rooms"].map { it["totalPrice"].asDouble() })
        assertEquals(listOf("pool", "wifi"), item["amenities"].map { it.asText() }.sorted())
        assertEquals(true, item["averageRating"].isNull)
    }

    @Test
    fun `given more guests than a room takes when searched then only the rooms that take them are offered`() {
        val hotel = aHotel(rooms = listOf(roomBody("Double", 2, 300, 2), roomBody("Suite", 4, 800, 1)))

        val rooms = json(search(hotel.destination, day(0), day(2), guests = 3))["items"][0]["rooms"]

        assertEquals(listOf("Suite"), rooms.map { it["name"].asText() })
        assertEquals(emptyList(), found(hotel.destination, day(0), day(2), guests = 5))
    }

    @Test
    fun `given a hotel at another destination when searched then it is not listed, and a bad search is a 400`() {
        val hotel = aHotel()

        assertEquals(emptyList(), found(newDestination(), day(0), day(2)))
        assertEquals(listOf(hotel.id), found(hotel.destination, day(0), day(2)))
        assertEquals(400, search(hotel.destination, day(2), day(2)).response.status)
        assertEquals(400, search(hotel.destination, day(3), day(2)).response.status)
        assertEquals(400, search(hotel.destination, day(0), day(2), guests = 0).response.status)
        assertEquals(400, search(hotel.destination, day(0), day(2), guests = 11).response.status)
        assertEquals(400, search(hotel.destination, java.time.LocalDate.now().minusDays(1), day(2)).response.status)
        assertEquals(400, search(hotel.destination, day(0), day(40)).response.status)
        assertEquals(400, search("gru1", day(0), day(2)).response.status)
        assertEquals(400, mockMvc.get("/v1/accommodations/search").andReturn().response.status)
    }

    @Test
    fun `given a hotel taken off sale when searched or opened then it is gone, and when put back it returns`() {
        val team = manager()
        val hotel = aHotel(team)

        adminPost(team, "/v1/admin/accommodations/${hotel.id}/deactivate")
        assertEquals(emptyList(), found(hotel.destination, day(0), day(2)))
        assertEquals(404, mockMvc.get("/v1/accommodations/${hotel.id}").andReturn().response.status)
        assertEquals(409, adminPost(team, "/v1/admin/accommodations/${hotel.id}/deactivate").response.status)

        adminPost(team, "/v1/admin/accommodations/${hotel.id}/activate")
        assertEquals(listOf(hotel.id), found(hotel.destination, day(0), day(2)))
        assertEquals(200, mockMvc.get("/v1/accommodations/${hotel.id}").andReturn().response.status)
    }

    @Test
    fun `given bad data, an unknown destination or a repeated room name when creating then 400, 404 or 409`() {
        val team = manager()
        val destination = newDestination()

        assertEquals(404, createHotel(team, "QQQ").response.status)
        assertEquals(400, createHotel(team, destination, rooms = listOf(roomBody(quantity = 0))).response.status)
        assertEquals(400, createHotel(team, destination, rooms = listOf(roomBody(rate = 0))).response.status)
        assertEquals(409, createHotel(team, destination, rooms = listOf(roomBody("A"), roomBody("a"))).response.status)
    }

    @Test
    fun `given support, a customer and nobody when managing hotels then 403, 403, 401, and the manager 201`() {
        val support = registerStaffAndLogin(uniqueEmail(), Role.SUPPORT)
        val destination = newDestination()

        assertEquals(403, createHotel(support, destination).response.status)
        assertEquals(403, createHotel(aCustomerToken(), destination).response.status)
        assertEquals(401, mockMvc.get("/v1/admin/accommodations").andReturn().response.status)
        assertEquals(201, createHotel(manager(), destination).response.status)
    }
}
