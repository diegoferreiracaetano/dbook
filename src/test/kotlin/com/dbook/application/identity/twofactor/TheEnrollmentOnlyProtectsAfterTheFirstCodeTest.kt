package com.dbook.application.identity.twofactor

import com.dbook.domain.common.audit.AuditAction
import com.dbook.domain.identity.InvalidTwoFactorCodeException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class TheEnrollmentOnlyProtectsAfterTheFirstCodeTest : TwoFactorFixture() {
    @Test
    fun `given an enrollment when it starts then the secret is stored encrypted and is not active`() {
        val start = enroll.execute(1)

        val stored = requireNotNull(repository.find(1))
        assertFalse(stored.isActive)
        assertTrue(stored.secretEncrypted.startsWith("v1:"))
        assertFalse(stored.secretEncrypted.contains(start.manualEntryKey), "the secret is not stored in the clear")
        assertEquals(start.manualEntryKey, totp.manualEntryKey(cipher.decrypt(stored.secretEncrypted)))
    }

    @Test
    fun `given a wrong first code when confirming then it fails and the enrollment can be tried again`() {
        enroll.execute(1)

        assertFailsWith<InvalidTwoFactorCodeException> { confirm.execute(rootActor, "123456") }

        assertEquals(10, confirm.execute(rootActor, codeOnThePhone()).size)
    }

    @Test
    fun `given a confirmed enrollment when it is confirmed again then it is a conflict and nothing changes`() {
        enrolled()

        assertFailsWith<IllegalStateException> { confirm.execute(rootActor, codeOnThePhone()) }
        assertFailsWith<IllegalStateException> { enroll.execute(1) }
    }

    @Test
    fun `given an enrollment that was not confirmed when it starts again then the secret is replaced`() {
        val first = enroll.execute(1)
        val second = enroll.execute(1)

        assertNotEquals(first.manualEntryKey, second.manualEntryKey)
    }

    @Test
    fun `given the first code when confirmed then it is spent, the codes are hashed and the audit says who`() {
        enroll.execute(1)
        val code = codeOnThePhone()

        val codes = confirm.execute(rootActor, code)

        assertEquals(10, codes.toSet().size)
        assertEquals(AuditAction.TWO_FACTOR_ENABLED, audit.events.single().action)
        assertEquals("1", audit.events.single().targetId)
        // the code that confirmed cannot open a session an instant later
        val challenge = challengeOf(staffLogin.execute(staffPassword()))
        assertFailsWith<InvalidTwoFactorCodeException> { verify.execute(verifyCommand(challenge, code)) }
    }
}
