package com.dbook.presentation.hardening

import com.dbook.presentation.securityintegration.SecurityIntegrationFixture
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.post
import kotlin.test.Test
import kotlin.test.assertEquals

class ABodyAboveTheLimitIsRefusedBeforeItIsReadTest : SecurityIntegrationFixture() {
    private fun login(body: String) =
        mockMvc.post("/v1/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = body
        }.andReturn()

    @Test
    fun `given a body of 300 KB when posted to an ordinary endpoint then it is a 413 in the API's format`() {
        val refused = login("""{"email":"${"a".repeat(300_000)}","password":"x"}""")

        assertEquals(413, refused.response.status)
        assertEquals("PAYLOAD_TOO_LARGE", objectMapper.readTree(refused.response.contentAsString)["code"].asText())
    }

    @Test
    fun `given a normal body when posted then it goes through to the endpoint`() {
        assertEquals(401, login("""{"email":"nobody@example.com","password":"wrong-password"}""").response.status)
    }

    @Test
    fun `given the same 300 KB body when posted to the CSV import then the size is not what stops it`() {
        val result =
            mockMvc.post("/v1/admin/flights/import") {
                contentType = MediaType.parseMediaType("text/csv")
                content = "x".repeat(300_000)
            }.andReturn()

        assertEquals(401, result.response.status)
    }
}
