package com.dbook.presentation.twofactor

import com.dbook.domain.identity.Role
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AnotherSuperAdminResetsALostSecondFactorTest : TwoFactorApiFixture() {
    private val lost = uniqueEmail()
    private val boss = uniqueEmail()

    private fun reset(
        token: String,
        targetId: Long,
        reason: String = "lost the phone and the recovery codes",
    ) = postJson("/v1/admin/staff/$targetId/2fa/reset", mapOf("reason" to reason), token)

    @Test
    fun `given a lost authenticator when another admin resets it then it is gone, audited, sessions ended`() {
        staffWithTwoFactor(lost)
        val cookie = refreshCookieOf(verify(challengeFor(lost), codeOnThePhone(lost)))
        val bossToken = registerStaffAndLogin(boss)

        val result = reset(bossToken, userIdOf(lost))

        assertEquals(204, result.response.status)
        assertNull(twoFactor.find(userIdOf(lost)))
        assertEquals(401, adminRefresh(cookie).andReturn().response.status, "their old session ended")
        assertEquals(200, adminLogin(lost).response.status, "and they sign in with the password alone again")
        val entry = auditEntries(bossToken, "action=TWO_FACTOR_RESET").first()
        assertEquals("lost the phone and the recovery codes", entry["reason"].asText())
        assertEquals(userIdOf(lost).toString(), entry["targetId"].asText())
    }

    @Test
    fun `given support staff when they try to reset then 403`() {
        staffWithTwoFactor(lost)
        val support = registerStaffAndLogin(uniqueEmail(), Role.SUPPORT)

        assertEquals(403, reset(support, userIdOf(lost)).response.status)
        assertEquals(true, twoFactor.find(userIdOf(lost))?.isActive)
    }

    @Test
    fun `given the account itself when it tries to reset its own then 409`() {
        staffWithTwoFactor(lost)
        val token = json(verify(challengeFor(lost), codeOnThePhone(lost)))["accessToken"].asText()

        assertEquals(409, reset(token, userIdOf(lost)).response.status)
        assertEquals(true, twoFactor.find(userIdOf(lost))?.isActive)
    }

    @Test
    fun `given a reason that is too short when resetting then 400`() {
        staffWithTwoFactor(lost)
        val bossToken = registerStaffAndLogin(boss)

        assertEquals(400, reset(bossToken, userIdOf(lost), reason = "lost").response.status)
    }
}
