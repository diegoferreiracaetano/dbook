package com.dbook.infrastructure.security

import com.dbook.domain.identity.InvitationTokenGenerator
import org.springframework.stereotype.Component
import java.security.SecureRandom
import java.util.Base64

@Component
class SecureRandomInvitationTokens : InvitationTokenGenerator {
    private val random = SecureRandom()

    override fun generate(): String {
        val bytes = ByteArray(TOKEN_BYTES).also(random::nextBytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private companion object {
        const val TOKEN_BYTES = 32
    }
}
