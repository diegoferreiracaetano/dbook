package com.dbook.domain

/** Hashes and verifies passwords — the domain never sees or compares raw passwords directly. */
interface PasswordHasher {
    fun hash(rawPassword: String): String

    fun matches(
        rawPassword: String,
        hash: String,
    ): Boolean
}
