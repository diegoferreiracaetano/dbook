package com.dbook.domain.identity

/** Persistence port for [User]. */
interface UserRepository {
    fun findById(id: Long): User?

    fun findByEmail(email: String): User?

    fun save(user: User): User
}
