package com.dbook.infrastructure.security

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * How the signing key rotates. `jwt.secret` is the **active** key (it signs, and its id goes in the `kid` header of
 * every token); [retired] holds the previous keys by id, which **only verify**, for as long as tokens signed with them
 * can still be alive (the access token's lifetime, then the refresh token's). Rotating is: move the old secret here
 * under its id, put a new secret in `jwt.secret` with a new [activeKeyId], deploy; later, drop the retired key.
 * A token with no `kid` (issued before rotation existed) is checked with the key called `legacy` if there is one,
 * and with the active key otherwise.
 */
@ConfigurationProperties(prefix = "jwt-keys")
data class JwtKeyProperties(
    val activeKeyId: String = "k1",
    val retired: Map<String, String> = emptyMap(),
)
