package com.dbook.infrastructure.security.twofactor

import com.dbook.infrastructure.security.Rfc6238TotpService
import kotlin.test.Test
import kotlin.test.assertEquals

// Appendix B of RFC 6238, the HMAC-SHA1 rows: the secret is the ASCII text "12345678901234567890" and the codes
// are 8 digits. Matching them proves the HMAC, the time step and the truncation, before any app depends on them.
class TheCodesFollowTheRfc6238TestVectorsTest {
    private val secret = "12345678901234567890".toByteArray()
    private val service = Rfc6238TotpService("DBook", digits = 8)

    @Test
    fun `given the RFC test times when the code is made then it is the one the RFC prints`() {
        val vectors =
            mapOf(
                59L to "94287082",
                1111111109L to "07081804",
                1111111111L to "14050471",
                1234567890L to "89005924",
                2000000000L to "69279037",
                20000000000L to "65353130",
            )

        vectors.forEach { (time, expected) -> assertEquals(expected, service.codeAt(secret, time / 30), "at $time") }
    }

    @Test
    fun `given six digits when the code is made then it is the last six of the eight`() {
        val six = Rfc6238TotpService("DBook")

        assertEquals("287082", six.codeAt(secret, 59 / 30))
    }
}
