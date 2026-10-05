package com.dbook.infrastructure.security

import com.dbook.domain.identity.RecoveryCodeGenerator
import org.springframework.stereotype.Component
import java.security.SecureRandom

/** `xxxxx-xxxxx`: ten characters of a Base32-like alphabet without look-alikes (no 0/O, 1/I), about 50 bits. */
@Component
class SecureRandomRecoveryCodes : RecoveryCodeGenerator {
    private val random = SecureRandom()

    override fun generate(): String =
        List(CODE_CHARS) { ALPHABET[random.nextInt(ALPHABET.length)] }.joinToString("").chunked(HALF).joinToString("-")

    private companion object {
        const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        const val CODE_CHARS = 10
        const val HALF = 5
    }
}
