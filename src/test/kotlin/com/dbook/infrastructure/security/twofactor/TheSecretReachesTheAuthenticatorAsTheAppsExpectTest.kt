package com.dbook.infrastructure.security.twofactor

import com.dbook.infrastructure.security.Rfc6238TotpService
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TheSecretReachesTheAuthenticatorAsTheAppsExpectTest {
    private val service = Rfc6238TotpService("DBook")

    @Test
    fun `given the RFC secret when written for typing then it is its Base32 text`() {
        // RFC 4648 section 10 style check: "12345678901234567890" in Base32
        assertEquals("GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ", service.manualEntryKey("12345678901234567890".toByteArray()))
    }

    @Test
    fun `given an account when the URI is made then it carries the issuer, the secret and the parameters`() {
        val secret = "12345678901234567890".toByteArray()

        val uri = service.otpauthUri(secret, "ana+ops@example.com")

        assertEquals(
            "otpauth://totp/DBook:ana%2Bops%40example.com?secret=GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ" +
                "&issuer=DBook&algorithm=SHA1&digits=6&period=30",
            uri,
        )
    }

    @Test
    fun `given two secrets when made then they are 20 random bytes each and differ`() {
        val first = service.newSecret()
        val second = service.newSecret()

        assertEquals(20, first.size)
        assertTrue(!first.contentEquals(second))
    }
}
