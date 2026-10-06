package com.dbook.presentation.accountrecovery

import org.springframework.test.web.servlet.get
import kotlin.test.Test
import kotlin.test.assertEquals

class TheEmailIsConfirmedWithTheLinkTest : AccountRecoveryApiFixture() {
    private val email = uniqueEmail()

    private fun profile(token: String) =
        json(mockMvc.get("/v1/users/me") { header("Authorization", "Bearer $token") }.andReturn())

    @Test
    fun `given a new account when it registers then it is unconfirmed and the link is mailed`() {
        val registered = register(email)

        assertEquals(201, registered.response.status)
        assertEquals(false, json(registered)["emailVerified"].asBoolean())
        tokenMailedTo(email, CONFIRM)
    }

    @Test
    fun `given the mailed link when it is used then 204 and the profile says confirmed`() {
        register(email)
        val token = tokenMailedTo(email, CONFIRM)
        val session = loginAccessToken(email, "s3cret-password")

        assertEquals(204, postJson("/v1/auth/verify-email", mapOf("token" to token)).response.status)

        assertEquals(true, profile(session)["emailVerified"].asBoolean())
    }

    @Test
    fun `given a link already used or made up when it is sent then 400 INVALID_ACCOUNT_TOKEN`() {
        register(email)
        val token = tokenMailedTo(email, CONFIRM)
        postJson("/v1/auth/verify-email", mapOf("token" to token))

        listOf(token, "made-up").forEach {
            val result = postJson("/v1/auth/verify-email", mapOf("token" to it))
            assertEquals(400, result.response.status)
            assertEquals("INVALID_ACCOUNT_TOKEN", errorCodeOf(result))
        }
    }

    @Test
    fun `given an unconfirmed customer when a new link is asked then the old one stops working`() {
        register(email)
        val old = tokenMailedTo(email, CONFIRM)
        val session = loginAccessToken(email, "s3cret-password")
        val before = mailbox.sent.count { it.to == email }

        assertEquals(204, postJson("/v1/auth/resend-verification", emptyMap(), session).response.status)

        assertEquals(before + 1, mailbox.sent.count { it.to == email })
        assertEquals(400, postJson("/v1/auth/verify-email", mapOf("token" to old)).response.status)
        assertEquals(
            204,
            postJson("/v1/auth/verify-email", mapOf("token" to tokenMailedTo(email, CONFIRM))).response.status,
        )
    }

    @Test
    fun `given a confirmed customer when a new link is asked then 409, and without a session 401`() {
        register(email)
        postJson("/v1/auth/verify-email", mapOf("token" to tokenMailedTo(email, CONFIRM)))
        val session = loginAccessToken(email, "s3cret-password")

        assertEquals(409, postJson("/v1/auth/resend-verification", emptyMap(), session).response.status)
        assertEquals(401, postJson("/v1/auth/resend-verification", emptyMap()).response.status)
    }

    @Test
    fun `given the mailbox when a registration is refused then no mail leaves`() {
        register(email)
        val sent = mailbox.sent.size

        assertEquals(409, register(email).response.status)

        assertEquals(sent, mailbox.sent.size)
    }
}
