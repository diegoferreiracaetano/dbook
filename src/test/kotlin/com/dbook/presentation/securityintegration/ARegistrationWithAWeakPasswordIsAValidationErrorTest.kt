package com.dbook.presentation.securityintegration

import org.springframework.http.MediaType
import org.springframework.test.web.servlet.post
import kotlin.test.Test
import kotlin.test.assertEquals

class ARegistrationWithAWeakPasswordIsAValidationErrorTest : SecurityIntegrationFixture() {
    @Test
    fun `given a common password when registering then it is 400 VALIDATION_FAILED`() {
        val result =
            mockMvc.post("/v1/auth/register") {
                contentType = MediaType.APPLICATION_JSON
                content =
                    objectMapper.writeValueAsString(
                        mapOf("email" to uniqueEmail(), "password" to "password123", "name" to "Weak"),
                    )
            }.andReturn()

        assertEquals(400, result.response.status)
        assertEquals("VALIDATION_FAILED", errorCodeOf(result))
    }
}
