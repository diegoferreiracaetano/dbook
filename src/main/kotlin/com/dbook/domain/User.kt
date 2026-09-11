package com.dbook.domain

/** An authenticated principal. Always registers as [Role.CLIENT] — no self-promotion endpoint exists on purpose. */
class User(
    val id: Long? = null,
    val email: String,
    val passwordHash: String,
    val role: Role = Role.CLIENT,
) {
    init {
        require(email.isNotBlank() && email.contains("@")) { "email must be a valid address" }
        require(passwordHash.isNotBlank()) { "passwordHash must not be blank" }
    }
}
