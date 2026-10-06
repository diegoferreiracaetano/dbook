package com.dbook.presentation.accommodation

import com.dbook.domain.common.access.Role
import com.dbook.presentation.adminbookings.AdminBookingsFixture
import com.fasterxml.jackson.databind.JsonNode
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import java.time.LocalDate
import java.util.UUID

/** A hotel with one Double room type (2 guests, 300.00, one room) at a destination of its own. */
data class Hotel(
    val id: Long,
    val destination: String,
    val roomTypeId: Long,
)

abstract class HotelApiFixture : AdminBookingsFixture() {
    protected fun manager(): String = registerStaffAndLogin(uniqueEmail(), Role.CATALOG_MANAGER)

    protected fun json(result: MvcResult): JsonNode =
        objectMapper.readTree(result.response.getContentAsString(Charsets.UTF_8))

    protected fun roomBody(
        name: String = "Double",
        capacity: Int = 2,
        rate: Any = 300,
        quantity: Int = 1,
    ) = mapOf("name" to name, "capacity" to capacity, "nightlyRate" to rate, "quantity" to quantity)

    protected fun hotelBody(
        destination: String,
        name: String = "Hotel ${UUID.randomUUID().toString().take(HOTEL_SUFFIX)}",
        stars: Int = 4,
    ) = mapOf(
        "name" to name,
        "destinationIataCode" to destination,
        "address" to "Av. Atlântica, 1000",
        "stars" to stars,
        "description" to "By the sea",
        "amenities" to listOf("WiFi", "Pool"),
    )

    protected fun createHotel(
        token: String,
        destination: String,
        rooms: List<Map<String, Any?>> = listOf(roomBody()),
    ): MvcResult =
        mockMvc.post("/v1/admin/accommodations") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content =
                objectMapper.writeValueAsString(mapOf("accommodation" to hotelBody(destination), "roomTypes" to rooms))
        }.andReturn()

    /** A destination and a hotel in it with one Double room, ready to book. */
    protected fun aHotel(
        token: String = manager(),
        rooms: List<Map<String, Any?>> = listOf(roomBody()),
    ): Hotel {
        val destination = newDestination()
        val created = json(createHotel(token, destination, rooms))
        return Hotel(created["id"].asLong(), destination, created["roomTypes"][0]["id"].asLong())
    }

    protected fun adminGet(
        token: String,
        url: String,
    ): MvcResult = mockMvc.get(url) { header("Authorization", "Bearer $token") }.andReturn()

    protected fun adminPost(
        token: String,
        url: String,
    ): MvcResult = mockMvc.post(url) { header("Authorization", "Bearer $token") }.andReturn()

    protected fun addRoomType(
        token: String,
        hotelId: Long,
        body: Map<String, Any?>,
    ): MvcResult =
        mockMvc.post("/v1/admin/accommodations/$hotelId/room-types") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(body)
        }.andReturn()

    protected fun adminPut(
        token: String,
        url: String,
        body: Map<String, Any?>,
    ): MvcResult =
        mockMvc.put(url) {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(body)
        }.andReturn()

    protected fun search(
        destination: String,
        checkIn: LocalDate,
        checkOut: LocalDate,
        guests: Int? = 2,
    ): MvcResult =
        mockMvc.get("/v1/accommodations/search") {
            param("destination", destination)
            param("checkIn", checkIn.toString())
            param("checkOut", checkOut.toString())
            guests?.let { param("guests", it.toString()) }
        }.andReturn()

    protected fun found(
        destination: String,
        checkIn: LocalDate,
        checkOut: LocalDate,
        guests: Int = 2,
    ): List<Long> = json(search(destination, checkIn, checkOut, guests))["items"].map { it["id"].asLong() }

    protected fun bookStay(
        token: String,
        hotel: Hotel,
        checkIn: LocalDate,
        checkOut: LocalDate,
        guests: Int = 2,
    ): MvcResult = bookRoomType(token, hotel.id, hotel.roomTypeId, checkIn to checkOut, guests)

    protected fun bookRoomType(
        token: String,
        hotelId: Long,
        roomTypeId: Long,
        nights: Pair<LocalDate, LocalDate>,
        guests: Int = 2,
    ): MvcResult =
        mockMvc.post("/v1/accommodations/$hotelId/bookings") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content =
                objectMapper.writeValueAsString(
                    mapOf(
                        "roomTypeId" to roomTypeId,
                        "checkIn" to nights.first.toString(),
                        "checkOut" to nights.second.toString(),
                        "guests" to guests,
                    ),
                )
        }.andReturn()

    protected fun aCustomerToken(): String = registerAndLogin(uniqueEmail())

    protected fun bookedNightsOf(roomTypeId: Long): Int =
        jdbcTemplate.queryForObject(
            "SELECT COALESCE(sum(booked), 0) FROM room_night WHERE room_type_id = ?", Int::class.java, roomTypeId,
        ) ?: 0

    protected fun day(offset: Long): LocalDate = BASE.plusDays(offset)

    private companion object {
        const val HOTEL_SUFFIX = 6
        val BASE: LocalDate = LocalDate.now().plusDays(90)
    }
}
