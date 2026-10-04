package com.dbook.domain.identity.passwordpolicy

import com.dbook.domain.identity.PasswordPolicy
import kotlin.test.Test
import kotlin.test.assertFailsWith

// BCrypt ignores everything after 72 bytes: two long passwords sharing their first 72 bytes would be the same one
class RejectsAPasswordBcryptWouldTruncateTest {
    @Test
    fun `given a password over 72 bytes when validated then it throws, counting bytes and not characters`() {
        assertFailsWith<IllegalArgumentException> { PasswordPolicy.validate("a".repeat(73), "diego@example.com") }
        assertFailsWith<IllegalArgumentException> { PasswordPolicy.validate("é".repeat(37), "diego@example.com") }
    }
}
