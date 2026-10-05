package com.dbook.presentation.twofactor

import com.dbook.domain.identity.Role
import com.dbook.domain.identity.SecretCipher
import com.dbook.domain.identity.TotpService
import com.dbook.domain.identity.TwoFactorRepository
import com.dbook.infrastructure.security.Rfc6238TotpService
import com.dbook.presentation.securityintegration.SecurityIntegrationFixture
import com.fasterxml.jackson.databind.JsonNode
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.time.Clock

abstract class TwoFactorApiFixture : SecurityIntegrationFixture() {
    @Autowired
    lateinit var twoFactor: TwoFactorRepository

    @Autowired
    lateinit var cipher: SecretCipher

    @Autowired
    lateinit var totp: TotpService

    @Autowired
    lateinit var clock: Clock

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

    protected fun getWith(
        url: String,
        token: String,
    ): MvcResult = mockMvc.get(url) { header("Authorization", "Bearer $token") }.andReturn()

    /**
     * What the authenticator shows now. The test setup forgets the step last used first: a code is good once, and
     * these tests ask for several within the same 30 seconds (the replay has its own test, which does not call this).
     */
    protected fun codeOnThePhone(email: String): String {
        val userId = userIdOf(email)
        jdbcTemplate.update("UPDATE staff_totp SET last_used_step = 0 WHERE user_id = ?", userId)
        val secret = cipher.decrypt(requireNotNull(twoFactor.find(userId)).secretEncrypted)
        return (totp as Rfc6238TotpService).codeAt(secret, clock.instant().epochSecond / STEP_SECONDS)
    }

    /** A staff member with the second factor on. Returns the recovery codes. */
    protected fun staffWithTwoFactor(
        email: String,
        role: Role = Role.SUPER_ADMIN,
    ): List<String> {
        val token = registerStaffAndLogin(email, role)
        postJson("/v1/admin/2fa/enroll", emptyMap(), token)
        val confirmed = postJson("/v1/admin/2fa/confirm", mapOf("code" to codeOnThePhone(email)), token)
        return json(confirmed)["recoveryCodes"].map { it.asText() }
    }

    /** The password step of the portal: a 202 with the challenge. */
    protected fun challengeFor(email: String): String {
        val result = adminLogin(email)
        check(result.response.status == 202) { "expected a challenge, got ${result.response.status}" }
        return json(result)["challengeToken"].asText()
    }

    protected fun verify(
        challenge: String,
        code: String,
    ): MvcResult = postJson("/v1/admin/auth/2fa/verify", mapOf("challengeToken" to challenge, "code" to code))

    private companion object {
        const val STEP_SECONDS = 30L
    }
}
