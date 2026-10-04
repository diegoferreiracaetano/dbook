package com.dbook.application.identity.bootstrapsuperadminusecase

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class RejectsAWeakPasswordTest : BootstrapSuperAdminUseCaseFixture() {
    @Test
    fun `given a password under the staff minimum when bootstrapping then it fails and creates nothing`() {
        assertFailsWith<IllegalArgumentException> { bootstrap(password = "short-11-ch") }

        assertNull(superAdmin())
    }
}
