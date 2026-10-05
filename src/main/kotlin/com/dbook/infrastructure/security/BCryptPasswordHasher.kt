package com.dbook.infrastructure.security

import com.dbook.domain.identity.PasswordHasher
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Component

/**
 * BCrypt with a cost of 12 by default (OWASP asks for at least 10; each step doubles the work, so 12 costs a login
 * a few hundred milliseconds, which an attacker with a stolen table pays for every guess). The cost is written
 * inside every hash, so raising it never breaks the hashes already stored: they keep verifying at their own cost.
 */
@Component
class BCryptPasswordHasher(
    @Value("\${security.bcrypt-strength:12}") strength: Int,
) : PasswordHasher {
    private val encoder = BCryptPasswordEncoder(strength)

    override fun hash(rawPassword: String): String = encoder.encode(rawPassword)

    override fun matches(
        rawPassword: String,
        hash: String,
    ): Boolean = encoder.matches(rawPassword, hash)
}
