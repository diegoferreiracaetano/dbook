package com.dbook.domain.identity.passwordpolicy

import com.dbook.domain.identity.PasswordPolicy
import kotlin.test.Test

class AcceptsAStrongPasswordTest {
    @Test
    fun `given a long unusual password when validated then it is accepted, spaces included`() {
        PasswordPolicy.validate("correct horse battery staple", "diego@example.com")
    }
}
