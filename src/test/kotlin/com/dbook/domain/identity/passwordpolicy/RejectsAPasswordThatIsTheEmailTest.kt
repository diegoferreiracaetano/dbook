package com.dbook.domain.identity.passwordpolicy

import com.dbook.domain.identity.PasswordPolicy
import kotlin.test.Test
import kotlin.test.assertFailsWith

class RejectsAPasswordThatIsTheEmailTest {
    @Test
    fun `given a password equal to the email regardless of case when validated then it throws`() {
        assertFailsWith<IllegalArgumentException> {
            PasswordPolicy.validate("Diego.Caetano@Example.com", "diego.caetano@example.com")
        }
    }
}
