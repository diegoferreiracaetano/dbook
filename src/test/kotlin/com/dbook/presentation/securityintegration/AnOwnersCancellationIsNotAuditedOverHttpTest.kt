package com.dbook.presentation.securityintegration

import org.springframework.http.MediaType
import org.springframework.test.web.servlet.post
import kotlin.test.Test
import kotlin.test.assertEquals

class AnOwnersCancellationIsNotAuditedOverHttpTest : SecurityIntegrationFixture() {
    @Test
    fun `given a customer cancelling their own booking when the trail is read then there is no entry for it`() {
        val ownerToken = registerAndLogin(uniqueEmail())
        val adminToken = registerStaffAndLogin(uniqueEmail())
        val (bookableId, seatId) = registerFlightWithOneSeat()
        val booking =
            mockMvc.post("/v1/bookings") {
                header("Authorization", "Bearer $ownerToken")
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(mapOf("bookableId" to bookableId, "seatId" to seatId))
            }.andReturn()
        val bookingId = objectMapper.readTree(booking.response.contentAsString)["id"].asLong()
        mockMvc.post("/v1/bookings/$bookingId/cancel") { header("Authorization", "Bearer $ownerToken") }
            .andExpect { status { isOk() } }

        val entries = auditEntries(adminToken, "action=BOOKING_CANCELLED_BY_STAFF&targetId=$bookingId")

        assertEquals(0, entries.size())
    }
}
