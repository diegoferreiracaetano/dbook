package com.dbook.domain.identity

import java.time.Instant

/** Persistence port for [User]. */
interface UserRepository {
    fun findById(id: Long): User?

    // ignores case: 'Maria@x.com' and 'maria@x.com' are the same mailbox
    fun findByEmail(email: String): User?

    fun save(user: User): User

    fun recordLogin(
        userId: Long,
        at: Instant,
    )

    fun findStaff(): List<User>

    fun existsByRole(role: Role): Boolean

    // locks the rows until the transaction ends: two requests that would each leave the other as the only
    // SUPER_ADMIN cannot both decide it is safe
    fun lockActiveByRole(role: Role): List<User>
}
