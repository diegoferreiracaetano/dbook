package com.dbook.domain.identity

// The addresses of anonymized accounts, kept only as hashes, so that registration can refuse them.
interface AnonymizedEmailRepository {
    fun remember(email: String)

    fun isRemembered(email: String): Boolean
}
