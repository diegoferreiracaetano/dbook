package com.dbook.domain.identity.passwordpolicy

import com.dbook.domain.identity.PasswordPolicy
import kotlin.test.Test
import kotlin.test.assertFailsWith

class RejectsACommonPasswordTest {
    @Test
    fun `given a well-known password long enough for the minimum when validated then it throws`() {
        assertFailsWith<IllegalArgumentException> { PasswordPolicy.validate("Password123", "diego@example.com") }
    }
}
