package com.dbook.presentation.securityintegration

import org.springframework.http.MediaType
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AuditPagesWithTheCursorOverHttpTest : SecurityIntegrationFixture() {
    @Test
    fun `given three audited flights when paging by two then the cursor leads to the third and then ends`() {
        val email = uniqueEmail()
        val token = registerStaffAndLogin(email)
        repeat(3) {
            mockMvc.post("/v1/admin/flights") {
                header("Authorization", "Bearer $token")
                contentType = MediaType.APPLICATION_JSON
                content = validFlightRequestBody()
            }.andExpect { status { isCreated() } }
        }
        val query = "actorId=${userIdOf(email)}&action=FLIGHT_CREATED&size=2"

        val first = page(token, query)
        val second = page(token, "$query&cursor=${first["nextCursor"].asText()}")

        assertEquals(2, first["items"].size())
        assertNotNull(first["nextCursor"].textValue())
        assertEquals(1, second["items"].size())
        assertNull(second["nextCursor"].textValue())
        assertTrue(first["items"][1]["id"].asLong() > second["items"][0]["id"].asLong(), "newest first, no repeat")
    }

    private fun page(
        token: String,
        query: String,
    ) = objectMapper.readTree(
        mockMvc.get("/v1/admin/audit?$query") { header("Authorization", "Bearer $token") }
            .andReturn().response.contentAsString,
    )
}
