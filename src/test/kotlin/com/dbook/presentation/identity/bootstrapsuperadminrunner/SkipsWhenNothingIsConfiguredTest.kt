package com.dbook.presentation.identity.bootstrapsuperadminrunner

import kotlin.test.Test
import kotlin.test.assertFalse

class SkipsWhenNothingIsConfiguredTest : BootstrapSuperAdminRunnerFixture() {
    @Test
    fun `given no bootstrap settings when starting then no account is created`() {
        runWith(email = "", password = "")

        assertFalse(hasSuperAdmin())
    }
}
