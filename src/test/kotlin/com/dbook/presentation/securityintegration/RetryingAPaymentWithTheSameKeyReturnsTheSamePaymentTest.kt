package com.dbook.presentation.securityintegration

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.web.servlet.post
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

// The scenario that motivated the Idempotency-Key: the first response is lost, the client
// sends the very same payment again. Without the key the retry would be a 409 (the booking
// is already CONFIRMED) even though the money was taken.
class RetryingAPaymentWithTheSameKeyReturnsTheSamePaymentTest : SecurityIntegrationFixture() {
    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @Test
    fun `given a paid booking when the same payment is sent again with its key then it returns the same one`() {
        val token = registerAndLogin(uniqueEmail())
        val (bookableId, seatId) = registerFlightWithOneSeat()
        val bookingResult =
            mockMvc.post("/v1/bookings") {
                header("Authorization", "Bearer $token")
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(mapOf("bookableId" to bookableId, "seatId" to seatId))
            }.andReturn()
        val bookingId = objectMapper.readTree(bookingResult.response.contentAsString)["id"].asLong()
        val key = UUID.randomUUID().toString()

        val first = pay(token, bookingId, key)
        val retry = pay(token, bookingId, key)

        assertEquals(first, retry)
        val paymentsWithKey =
            jdbcTemplate.queryForObject("SELECT count(*) FROM payment WHERE idempotency_key = ?", Long::class.java, key)
        assertEquals(1L, paymentsWithKey)
    }

    private fun pay(
        token: String,
        bookingId: Long,
        key: String,
    ): String =
        mockMvc.post("/v1/payments") {
            header("Authorization", "Bearer $token")
            header("Idempotency-Key", key)
            contentType = MediaType.APPLICATION_JSON
            content =
                objectMapper.writeValueAsString(
                    mapOf("bookingIds" to listOf(bookingId), "cardLast4" to "4242", "cardholderName" to "Jane Doe"),
                )
        }.andExpect {
            status { isCreated() }
        }.andReturn().response.contentAsString
}
