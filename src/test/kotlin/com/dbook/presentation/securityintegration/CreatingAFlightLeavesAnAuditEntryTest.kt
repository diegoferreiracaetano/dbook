package com.dbook.presentation.securityintegration

import org.springframework.http.MediaType
import org.springframework.test.web.servlet.post
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CreatingAFlightLeavesAnAuditEntryTest : SecurityIntegrationFixture() {
    @Test
    fun `given an admin creating a flight when the trail is read then the entry says who, what and which request`() {
        val email = uniqueEmail()
        val token = registerStaffAndLogin(email)
        val body = validFlightRequestBody()
        val created =
            mockMvc.post("/v1/admin/flights") {
                header("Authorization", "Bearer $token")
                contentType = MediaType.APPLICATION_JSON
                content = body
            }.andReturn()
        val flightId = objectMapper.readTree(created.response.contentAsString)["id"].asLong()

        val entries = auditEntries(token, "actorId=${userIdOf(email)}&action=FLIGHT_CREATED")

        assertEquals(1, entries.size())
        val entry = entries[0]
        assertEquals(flightId.toString(), entry["targetId"].asText())
        assertEquals("FLIGHT", entry["targetType"].asText())
        assertEquals("SUCCESS", entry["outcome"].asText())
        assertEquals(objectMapper.readTree(body)["flightNumber"].asText(), entry["after"]["flightNumber"].asText())
        assertTrue(entry["before"].isNull)
        assertFalse(entry["requestId"].isNull, "the entry must be tied to the request that caused it")
        assertFalse(entry["ip"].isNull)
    }
}
