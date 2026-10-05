package com.dbook.presentation.accommodation

import org.springframework.http.MediaType
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import kotlin.test.Test
import kotlin.test.assertEquals

class AHotelIsReviewedByItsGuestsAndRatedInTheSearchTest : HotelApiFixture() {
    private fun postReview(
        token: String,
        booking: Long,
        rating: Int,
    ) = mockMvc.post("/v1/reviews") {
        header("Authorization", "Bearer $token")
        contentType = MediaType.APPLICATION_JSON
        content =
            objectMapper.writeValueAsString(mapOf("bookingId" to booking, "rating" to rating, "comment" to "Nice stay"))
    }.andReturn()

    @Test
    fun `given paid stays when the guests review then the hotel, its destination and the search show the rating`() {
        val hotel = aHotel(rooms = listOf(roomBody(quantity = 3)))
        listOf(5, 3).forEachIndexed { index, rating ->
            val token = registerAndLogin(uniqueEmail(), name = "Ana Silva")
            val booking = json(bookStay(token, hotel, day(index * 10L), day(index * 10L + 2)))["id"].asLong()
            pay(token, booking)
            assertEquals(201, postReview(token, booking, rating).response.status)
        }

        val ofHotel = json(mockMvc.get("/v1/accommodations/${hotel.id}/reviews").andReturn())
        val ofDestination = json(mockMvc.get("/v1/destinations/${hotel.destination}/reviews").andReturn())
        val inSearch = json(search(hotel.destination, day(30), day(32)))["items"][0]

        assertEquals(2, ofHotel["summary"]["total"].asInt())
        assertEquals(4.0, ofHotel["summary"]["average"].asDouble())
        assertEquals("Ana S.", ofHotel["reviews"]["items"][0]["author"].asText())
        assertEquals(2, ofDestination["summary"]["total"].asInt())
        assertEquals(4.0, inSearch["averageRating"].asDouble())
        assertEquals(2, inSearch["reviewCount"].asInt())
    }

    @Test
    fun `given a booking that is not paid when its guest reviews then it is refused, like for a flight`() {
        val hotel = aHotel()
        val token = aCustomerToken()
        val booking = json(bookStay(token, hotel, day(0), day(2)))["id"].asLong()

        assertEquals(409, postReview(token, booking, 5).response.status)
    }

    @Test
    fun `given an unknown hotel when reading its reviews then it is a 404`() {
        assertEquals(404, mockMvc.get("/v1/accommodations/999999999/reviews").andReturn().response.status)
    }
}
