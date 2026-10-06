package com.dbook.presentation.accountrecovery

import com.dbook.application.identity.staff.RecordingEmailSender
import com.dbook.presentation.securityintegration.SecurityIntegrationFixture
import com.fasterxml.jackson.databind.JsonNode
import org.awaitility.Awaitility.await
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.post
import java.time.Duration

abstract class AccountRecoveryApiFixture : SecurityIntegrationFixture() {
    @Autowired
    lateinit var mailbox: RecordingEmailSender

    protected fun json(result: MvcResult): JsonNode =
        objectMapper.readTree(result.response.getContentAsString(Charsets.UTF_8))

    protected fun postJson(
        url: String,
        body: Map<String, Any?>,
        token: String? = null,
    ): MvcResult =
        mockMvc.post(url) {
            if (token != null) header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(body)
        }.andReturn()

    protected fun register(
        email: String,
        password: String = "s3cret-password",
    ): MvcResult = postJson("/v1/auth/register", mapOf("email" to email, "password" to password, "name" to "Ana"))

    /**
     * The token in the link of the newest mail to [email]. The reset mail leaves on another thread (so that the answer
     * does not tell whether the account exists): the test waits for it, as a customer waits for their inbox.
     */
    protected fun tokenMailedTo(
        email: String,
        subject: String,
    ): String {
        await().atMost(Duration.ofSeconds(5)).until { mailbox.sent.any { it.to == email && it.subject == subject } }
        return mailbox.sent.last { it.to == email && it.subject == subject }
            .body.substringAfter("token=").takeWhile { !it.isWhitespace() }
    }

    protected fun resetMailsTo(email: String): Int = mailbox.sent.count { it.to == email && it.subject == RESET }

    protected fun requestReset(email: String): MvcResult = postJson("/v1/auth/forgot-password", mapOf("email" to email))

    protected companion object {
        const val RESET = "Escolha uma nova senha no DBook"
        const val CONFIRM = "Confirme o seu e-mail no DBook"
    }
}
