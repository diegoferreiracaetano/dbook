package com.dbook.domain.identity

import java.time.Instant

/** Persistence port for [User]. */
interface UserRepository {
    fun findById(id: Long): User?

    fun findByEmail(email: String): User?

    fun save(user: User): User

    fun recordLogin(
        userId: Long,
        at: Instant,
    )
}
