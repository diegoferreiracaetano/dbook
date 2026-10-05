package com.dbook.infrastructure.security.twofactor

import com.dbook.infrastructure.security.AesGcmSecretCipher
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class TheStoredSecretIsUnreadableAndTamperEvidentTest {
    private val cipher = AesGcmSecretCipher("a-long-random-text-for-the-tests-0123456789")
    private val secret = "12345678901234567890".toByteArray()

    @Test
    fun `given a secret when encrypted and decrypted then it comes back the same`() {
        assertContentEquals(secret, cipher.decrypt(cipher.encrypt(secret)))
    }

    @Test
    fun `given the same secret when encrypted twice then the stored texts differ and hide it`() {
        val first = cipher.encrypt(secret)
        val second = cipher.encrypt(secret)

        assertNotEquals(first, second)
        assertTrue(first.startsWith("v1:"))
        assertTrue(
            !Base64.getDecoder().decode(first.removePrefix("v1:")).toString(Charsets.ISO_8859_1).contains("1234567890"),
        )
    }

    @Test
    fun `given a stored text changed by one bit when decrypted then it fails instead of returning something else`() {
        val bytes = Base64.getDecoder().decode(cipher.encrypt(secret).removePrefix("v1:"))
        bytes[bytes.size - 1] = (bytes.last().toInt() xor 1).toByte()

        assertFailsWith<Exception> { cipher.decrypt("v1:" + Base64.getEncoder().encodeToString(bytes)) }
    }

    @Test
    fun `given another key when decrypted then it fails`() {
        val other = AesGcmSecretCipher("another-long-random-text-for-the-tests-9876543210")

        assertFailsWith<Exception> { other.decrypt(cipher.encrypt(secret)) }
    }

    @Test
    fun `given a key that is too short when the cipher is built then it is refused`() {
        assertFailsWith<IllegalArgumentException> { AesGcmSecretCipher("short") }
    }

    @Test
    fun `given a text in an unknown format when decrypted then it is refused`() {
        assertFailsWith<IllegalArgumentException> { cipher.decrypt("v9:AAAA") }
    }
}
