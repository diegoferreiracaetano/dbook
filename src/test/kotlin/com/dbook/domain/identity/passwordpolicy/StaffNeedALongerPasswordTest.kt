package com.dbook.domain.identity.passwordpolicy

import com.dbook.domain.identity.PasswordPolicy
import kotlin.test.Test
import kotlin.test.assertFailsWith

class StaffNeedALongerPasswordTest {
    @Test
    fun `given a password that is fine for a client when validated for staff then it throws`() {
        PasswordPolicy.validate("eleven-char", "diego@example.com", PasswordPolicy.CLIENT_MIN_LENGTH)

        assertFailsWith<IllegalArgumentException> {
            PasswordPolicy.validate("eleven-char", "diego@example.com", PasswordPolicy.STAFF_MIN_LENGTH)
        }
    }
}
