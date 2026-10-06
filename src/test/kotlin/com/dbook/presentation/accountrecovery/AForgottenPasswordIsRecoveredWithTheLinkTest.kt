package com.dbook.presentation.accountrecovery

import org.awaitility.Awaitility.await
import org.springframework.test.web.servlet.post
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals

class AForgottenPasswordIsRecoveredWithTheLinkTest : AccountRecoveryApiFixture() {
    private val email = uniqueEmail()

    private fun reset(
        token: String,
        password: String,
    ) = postJson("/v1/auth/reset-password", mapOf("token" to token, "newPassword" to password))

    private fun login(password: String) =
        mockMvc.post("/v1/auth/login") {
            contentType = org.springframework.http.MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(mapOf("email" to email, "password" to password))
        }.andReturn().response.status

    @Test
    fun `given an account and an unknown address when the reset is asked then both answer 202 with the same body`() {
        register(email)

        val known = requestReset(email)
        val unknown = requestReset("nobody-${uniqueEmail()}")

        assertEquals(202, known.response.status)
        assertEquals(202, unknown.response.status)
        assertEquals(known.response.contentAsString, unknown.response.contentAsString)
    }

    @Test
    fun `given the mailed link when the password is chosen then only the new password and no old session work`() {
        register(email)
        val oldRefresh = loginRefreshToken(email, "s3cret-password")
        requestReset(email)
        val token = tokenMailedTo(email, RESET)

        val done = reset(token, "a-brand-new-password")

        assertEquals(204, done.response.status)
        assertEquals(200, login("a-brand-new-password"))
        assertEquals(401, login("s3cret-password"))
        assertEquals(401, postJson("/v1/auth/refresh", mapOf("refreshToken" to oldRefresh)).response.status)
    }

    @Test
    fun `given a link already used or made up when it is sent then 400 INVALID_ACCOUNT_TOKEN`() {
        register(email)
        requestReset(email)
        val token = tokenMailedTo(email, RESET)
        reset(token, "a-brand-new-password")

        listOf(token, "made-up").forEach {
            val result = reset(it, "another-new-password")
            assertEquals(400, result.response.status)
            assertEquals("INVALID_ACCOUNT_TOKEN", errorCodeOf(result))
        }
    }

    @Test
    fun `given a weak password when it is sent then 400 and the link still works`() {
        register(email)
        requestReset(email)
        val token = tokenMailedTo(email, RESET)

        val weak = reset(token, "short")

        assertEquals(400, weak.response.status)
        assertEquals("VALIDATION_FAILED", errorCodeOf(weak))
        assertEquals(204, reset(token, "a-brand-new-password").response.status)
    }

    @Test
    fun `given a blocked account when the reset is asked then 202 and nothing is mailed`() {
        register(email)
        block(email)

        assertEquals(202, requestReset(email).response.status)

        // the mail would leave on another thread: give it the time it would have needed
        await().pollDelay(Duration.ofMillis(500)).atMost(Duration.ofSeconds(2)).until { true }
        assertEquals(0, resetMailsTo(email))
    }

    @Test
    fun `given the same address asked too many times when one more comes then 429`() {
        register(email)

        val statuses = List(7) { requestReset(email).response.status }

        assertEquals(true, 429 in statuses, "the limit is on who asks: $statuses")
        assertEquals(202, statuses.first())
    }

    @Test
    fun `given a customer who was locked out by wrong passwords when the link is used then they can sign in again`() {
        register(email)
        repeat(6) { login("wrong-password") }
        assertEquals(429, login("s3cret-password"))
        requestReset(email)

        reset(tokenMailedTo(email, RESET), "a-brand-new-password")

        assertEquals(200, login("a-brand-new-password"))
    }
}
