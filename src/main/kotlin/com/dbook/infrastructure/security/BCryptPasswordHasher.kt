package com.dbook.infrastructure.security

import com.dbook.domain.PasswordHasher
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Component

@Component
class BCryptPasswordHasher(
    private val encoder: BCryptPasswordEncoder = BCryptPasswordEncoder(),
) : PasswordHasher {
    override fun hash(rawPassword: String): String = encoder.encode(rawPassword)

    override fun matches(
        rawPassword: String,
        hash: String,
    ): Boolean = encoder.matches(rawPassword, hash)
}
