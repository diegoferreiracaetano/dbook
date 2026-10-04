package com.dbook.presentation.securityintegration

import org.springframework.http.MediaType
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import kotlin.test.Test

class DestinationsShowTheAverageRatingTest : SecurityIntegrationFixture() {
    @Test
    fun `given two reviews for a dest when listing destinations then it returns their average without a token`() {
        val (firstBookableId, firstSeatId) = registerFlightWithOneSeat(destinationIataCode = "NRT")
        val (secondBookableId, secondSeatId) = registerFlightWithOneSeat(destinationIataCode = "NRT")
        bookPayAndReview(registerAndLogin(uniqueEmail()), firstBookableId, firstSeatId, rating = 5)
        bookPayAndReview(registerAndLogin(uniqueEmail()), secondBookableId, secondSeatId, rating = 3)

        mockMvc.get("/v1/destinations").andExpect {
            status { isOk() }
            jsonPath("$[?(@.iataCode == 'NRT')].averageRating") { value(4.0) }
        }
    }

    private fun bookPayAndReview(
        token: String,
        bookableId: Long,
        seatId: Long,
        rating: Int,
    ) {
        val bookingResult =
            mockMvc.post("/v1/bookings") {
                header("Authorization", "Bearer $token")
                header("Idempotency-Key", "test-payment-key")
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(mapOf("bookableId" to bookableId, "seatId" to seatId))
            }.andReturn()
        val bookingId = objectMapper.readTree(bookingResult.response.contentAsString)["id"].asLong()

        mockMvc.post("/v1/payments") {
            header("Authorization", "Bearer $token")
            header("Idempotency-Key", "test-payment-key")
            contentType = MediaType.APPLICATION_JSON
            content =
                objectMapper.writeValueAsString(
                    mapOf("bookingIds" to listOf(bookingId), "cardLast4" to "4242", "cardholderName" to "Jane Doe"),
                )
        }

        mockMvc.post("/v1/reviews") {
            header("Authorization", "Bearer $token")
            header("Idempotency-Key", "test-payment-key")
            contentType = MediaType.APPLICATION_JSON
            content =
                objectMapper.writeValueAsString(
                    mapOf("bookingId" to bookingId, "rating" to rating, "comment" to "Great flight!"),
                )
        }
    }
}
