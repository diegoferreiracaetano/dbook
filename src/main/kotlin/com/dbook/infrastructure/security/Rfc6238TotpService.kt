package com.dbook.infrastructure.security

import com.dbook.domain.identity.TotpService
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.net.URLEncoder
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * RFC 6238 on top of RFC 4226: HMAC-SHA1 (from the JVM, no cryptography written here) of the 30-second time step,
 * cut down to [digits] digits. The step a code was made for is what the caller records to refuse a second use.
 * [digits] is 6 (what authenticator apps expect); the RFC's own test vectors use 8, hence the parameter.
 */
@Component
class Rfc6238TotpService(
    @Value("\${admin.two-factor.issuer:DBook}") private val issuer: String,
    private val digits: Int = DEFAULT_DIGITS,
) : TotpService {
    private val random = SecureRandom()

    override fun newSecret(): ByteArray = ByteArray(SECRET_BYTES).also(random::nextBytes)

    override fun matchingStep(
        secret: ByteArray,
        code: String,
        now: Instant,
        afterStep: Long,
    ): Long? {
        val current = now.epochSecond / STEP_SECONDS
        return (current - TOLERANCE_STEPS..current + TOLERANCE_STEPS).firstOrNull { step ->
            step > afterStep && sameCode(codeAt(secret, step), code)
        }
    }

    /** The code for a time step. */
    fun codeAt(
        secret: ByteArray,
        step: Long,
    ): String {
        val mac = Mac.getInstance(HMAC).apply { init(SecretKeySpec(secret, HMAC)) }
        val hash = mac.doFinal(ByteBuffer.allocate(Long.SIZE_BYTES).putLong(step).array())
        val offset = hash.last().toInt() and LOW_NIBBLE
        val binary = ByteBuffer.wrap(hash, offset, Int.SIZE_BYTES).int and Int.MAX_VALUE
        return (binary % POWERS_OF_TEN.getValue(digits)).toString().padStart(digits, '0')
    }

    override fun otpauthUri(
        secret: ByteArray,
        account: String,
    ): String {
        val label = encode(issuer) + ":" + encode(account)
        return "otpauth://totp/$label?secret=${Base32.encode(secret)}&issuer=${encode(issuer)}" +
            "&algorithm=SHA1&digits=$digits&period=$STEP_SECONDS"
    }

    override fun manualEntryKey(secret: ByteArray): String = Base32.encode(secret)

    // compared in constant time: the code is a secret for 30 seconds
    private fun sameCode(
        expected: String,
        given: String,
    ): Boolean = MessageDigest.isEqual(expected.toByteArray(), given.trim().toByteArray())

    private fun encode(text: String) = URLEncoder.encode(text, Charsets.UTF_8).replace("+", "%20")

    private companion object {
        const val HMAC = "HmacSHA1"
        const val DEFAULT_DIGITS = 6
        const val SECRET_BYTES = 20
        const val STEP_SECONDS = 30L
        const val TOLERANCE_STEPS = 1L
        const val LOW_NIBBLE = 0x0f
        val POWERS_OF_TEN = mapOf(6 to 1_000_000, 8 to 100_000_000)
    }
}
