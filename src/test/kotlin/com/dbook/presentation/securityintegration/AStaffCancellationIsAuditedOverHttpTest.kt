package com.dbook.presentation.securityintegration

import org.springframework.http.MediaType
import org.springframework.test.web.servlet.post
import kotlin.test.Test
import kotlin.test.assertEquals

class AStaffCancellationIsAuditedOverHttpTest : SecurityIntegrationFixture() {
    @Test
    fun `given staff cancelling another user's booking when the trail is read then it has both states`() {
        val ownerToken = registerAndLogin(uniqueEmail())
        val staffEmail = uniqueEmail()
        val staffToken = registerStaffAndLogin(staffEmail)
        val (bookableId, seatId) = registerFlightWithOneSeat()
        val booking =
            mockMvc.post("/v1/bookings") {
                header("Authorization", "Bearer $ownerToken")
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(mapOf("bookableId" to bookableId, "seatId" to seatId))
            }.andReturn()
        val bookingId = objectMapper.readTree(booking.response.contentAsString)["id"].asLong()
        mockMvc.post("/v1/bookings/$bookingId/cancel") { header("Authorization", "Bearer $staffToken") }
            .andExpect { status { isOk() } }

        val entries = auditEntries(staffToken, "action=BOOKING_CANCELLED_BY_STAFF&targetId=$bookingId")

        assertEquals(1, entries.size())
        assertEquals(userIdOf(staffEmail), entries[0]["actorId"].asLong())
        assertEquals("PENDING", entries[0]["before"]["status"].asText())
        assertEquals("CANCELLED", entries[0]["after"]["status"].asText())
    }
}
