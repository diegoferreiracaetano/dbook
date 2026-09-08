package com.dbook.domain

interface UserRepository {
    fun findById(id: Long): User?

    fun findByEmail(email: String): User?

    fun save(user: User): User
}
