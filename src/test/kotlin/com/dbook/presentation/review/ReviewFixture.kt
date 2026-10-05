package com.dbook.presentation.review

import com.dbook.application.catalog.RegisterFlightCommand
import com.dbook.domain.catalog.SeatClass
import com.dbook.domain.identity.Actor
import com.dbook.domain.identity.Role
import com.dbook.presentation.customerprofile.CustomerProfileFixture
import com.fasterxml.jackson.databind.JsonNode
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import java.math.BigDecimal
import java.time.LocalDateTime

/** A customer who flew and wrote a review: their token, their id, and the review's id. */
data class Reviewer(
    val token: String,
    val customerId: Long,
    val reviewId: Long,
)

// Every test gets a destination of its own (a new airport), so what it counts is only what it created, however many
// other tests reviewed other destinations in the same database.
abstract class ReviewFixture : CustomerProfileFixture() {
    protected fun body(result: MvcResult): JsonNode =
        objectMapper.readTree(result.response.getContentAsString(Charsets.UTF_8))

    /** A customer named [name] who booked and paid a flight to [destination] and reviewed it. */
    protected fun reviewer(
        destination: String,
        rating: Int,
        comment: String = "A fine trip",
        name: String = "Maria Silva",
    ): Reviewer {
        val email = uniqueEmail()
        val token = registerAndLogin(email, name = name)
        val flight =
            registerFlightUseCase.execute(
                RegisterFlightCommand(
                    actor = Actor(id = 1, role = Role.SUPER_ADMIN),
                    flightNumber = "DBR${(10000..99999).random()}",
                    airlineIataCode = "LA",
                    originIataCode = "GRU",
                    destinationIataCode = destination,
                    departureTime = LocalDateTime.of(2027, 1, 1, 8, 0),
                    arrivalTime = LocalDateTime.of(2027, 1, 1, 9, 10),
                    seatClass = SeatClass.ECONOMY,
                    price = BigDecimal("100.00"),
                    totalCapacity = 1,
                    aircraftType = "Airbus A320",
                ),
            )
        val bookableId = requireNotNull(flight.id)
        val booking = book(token, bookableId, requireNotNull(seatRepository.findByBookableId(bookableId).first().id))
        pay(token, booking)
        val id = body(postReview(token, booking, rating, comment))["id"].asLong()
        return Reviewer(token, userIdOf(email), id)
    }

    protected fun postReview(
        token: String,
        bookingId: Long,
        rating: Int,
        comment: String,
    ): MvcResult =
        mockMvc.post("/v1/reviews") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content =
                objectMapper.writeValueAsString(
                    mapOf("bookingId" to bookingId, "rating" to rating, "comment" to comment),
                )
        }.andReturn()

    protected fun publicReviews(
        destination: String,
        vararg params: Pair<String, String>,
    ): MvcResult =
        mockMvc.get("/v1/destinations/$destination/reviews") { params.forEach { (name, value) -> param(name, value) } }
            .andReturn()

    protected fun editReview(
        token: String,
        id: Long,
        changes: Map<String, Any?>,
    ): MvcResult =
        mockMvc.patch("/v1/reviews/$id") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(changes)
        }.andReturn()

    protected fun deleteReview(
        token: String,
        id: Long,
    ): MvcResult = mockMvc.delete("/v1/reviews/$id") { header("Authorization", "Bearer $token") }.andReturn()

    protected fun reportReview(
        token: String,
        id: Long,
        reason: String = "Offensive language",
    ): MvcResult = postReason(token, "/v1/reviews/$id/report", reason)

    protected fun hideReview(
        staff: String,
        id: Long,
        reason: String = "Offensive language towards the crew",
    ): MvcResult = postReason(staff, "/v1/admin/reviews/$id/hide", reason)

    protected fun moderate(
        staff: String,
        id: Long,
        action: String,
    ): MvcResult =
        mockMvc.post("/v1/admin/reviews/$id/$action") { header("Authorization", "Bearer $staff") }.andReturn()

    protected fun adminReviews(
        staff: String,
        status: String?,
    ): MvcResult =
        mockMvc.get("/v1/admin/reviews") {
            header("Authorization", "Bearer $staff")
            status?.let { param("status", it) }
            param("size", "100")
        }.andReturn()

    protected fun queueIds(
        staff: String,
        status: String,
    ): List<Long> = body(adminReviews(staff, status))["items"].map { it["id"].asLong() }

    protected fun support(): String = registerStaffAndLogin(uniqueEmail(), Role.SUPPORT)

    private fun postReason(
        token: String,
        url: String,
        reason: String,
    ): MvcResult =
        mockMvc.post(url) {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(mapOf("reason" to reason))
        }.andReturn()
}
