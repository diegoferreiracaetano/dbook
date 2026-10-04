package com.dbook.domain.identity

object PasswordPolicy {
    const val CLIENT_MIN_LENGTH = 8
    const val STAFF_MIN_LENGTH = 12

    // BCrypt silently ignores everything past 72 bytes
    private const val MAX_BYTES = 72

    private val commonPasswords =
        setOf(
            "password1", "password12", "password123", "password1234", "passw0rd123", "123456789", "1234567890",
            "12345678910", "qwertyuiop", "qwerty123", "qwerty1234", "1q2w3e4r5t", "1q2w3e4r", "iloveyou1",
            "iloveyou12", "abc123456", "abcd123456", "admin1234", "admin12345", "welcome123", "welcome1234",
            "letmein123", "letmein1234", "changeme123", "trustno1234", "monkey1234", "dragon1234", "football123",
            "baseball123", "master1234", "senha12345", "senha123456", "mudar12345", "brasil12345", "123123123",
            "123456789a", "a123456789", "987654321", "0123456789", "111111111", "000000000", "aaaaaaaaa",
        )

    fun validate(
        password: String,
        email: String,
        minLength: Int = CLIENT_MIN_LENGTH,
    ) {
        require(password.length >= minLength) { "password must have at least $minLength characters" }
        require(password.toByteArray().size <= MAX_BYTES) { "password must have at most $MAX_BYTES bytes" }
        require(!password.equals(email, ignoreCase = true)) { "password must not be the email" }
        require(password.lowercase() !in commonPasswords) { "password is too common" }
    }
}
