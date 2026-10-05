package com.dbook.infrastructure.security

import com.dbook.domain.identity.SecretCipher
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES-256-GCM (from the JVM): confidential **and** tamper-evident, so a stored secret that was changed fails to
 * decrypt instead of decrypting to something else. Each value gets its own random IV, kept in front of the
 * ciphertext. The stored text starts with a version (`v1:`) so a future key can be told from this one.
 * The key comes from `totp.encryption-key`, a long random text from the environment (Secrets Manager in production):
 * the AES key is its SHA-256, so any text of enough length makes a key of exactly the size AES-256 needs.
 */
@Component
class AesGcmSecretCipher(
    @Value("\${totp.encryption-key}") keyText: String,
) : SecretCipher {
    private val key: SecretKeySpec
    private val random = SecureRandom()

    init {
        require(keyText.length >= MIN_KEY_CHARS) { "totp.encryption-key must have at least $MIN_KEY_CHARS characters" }
        key = SecretKeySpec(MessageDigest.getInstance("SHA-256").digest(keyText.toByteArray()), "AES")
    }

    override fun encrypt(plain: ByteArray): String {
        val iv = ByteArray(IV_BYTES).also(random::nextBytes)
        val cipher =
            Cipher.getInstance(TRANSFORMATION).apply {
                init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
            }
        return VERSION + Base64.getEncoder().encodeToString(iv + cipher.doFinal(plain))
    }

    override fun decrypt(stored: String): ByteArray {
        require(stored.startsWith(VERSION)) { "unknown secret format" }
        val bytes = Base64.getDecoder().decode(stored.removePrefix(VERSION))
        val cipher =
            Cipher.getInstance(TRANSFORMATION).apply {
                init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, bytes.copyOfRange(0, IV_BYTES)))
            }
        return cipher.doFinal(bytes, IV_BYTES, bytes.size - IV_BYTES)
    }

    private companion object {
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val VERSION = "v1:"
        const val MIN_KEY_CHARS = 32
        const val IV_BYTES = 12
        const val TAG_BITS = 128
    }
}
