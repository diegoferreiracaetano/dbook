package com.dbook.domain.identity.passwordpolicy

import com.dbook.domain.identity.PasswordPolicy
import kotlin.test.Test
import kotlin.test.assertFailsWith

class RejectsAShortPasswordTest {
    @Test
    fun `given a password below the client minimum when validated then it throws IllegalArgumentException`() {
        assertFailsWith<IllegalArgumentException> { PasswordPolicy.validate("1234567", "diego@example.com") }
    }
}
