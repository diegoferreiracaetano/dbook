package com.dbook.presentation.securityintegration

import org.springframework.http.MediaType
import org.springframework.test.web.servlet.post
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class PayingAfterARepriceChargesTheFrozenPriceTest : SecurityIntegrationFixture() {
    @Test
    fun `given a booking made at 100 when the flight is repriced to 150 then paying charges 100`() {
        val token = registerAndLogin(uniqueEmail())
        val (bookableId, seatId) = registerFlightWithOneSeat()
        val bookingId = book(token, bookableId, seatId)
        reprice(bookableId, "150.00")

        val payment =
            mockMvc.post("/v1/payments") {
                header("Authorization", "Bearer $token")
                header("Idempotency-Key", UUID.randomUUID().toString())
                contentType = MediaType.APPLICATION_JSON
                content =
                    objectMapper.writeValueAsString(
                        mapOf("bookingIds" to listOf(bookingId), "cardLast4" to "4242", "cardholderName" to "Jane Doe"),
                    )
            }.andReturn()

        assertEquals(201, payment.response.status)
        assertEquals(100.0, objectMapper.readTree(payment.response.contentAsString)["amount"].asDouble())
    }
}
