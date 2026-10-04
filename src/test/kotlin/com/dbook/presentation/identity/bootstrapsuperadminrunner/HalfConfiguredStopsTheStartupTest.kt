package com.dbook.presentation.identity.bootstrapsuperadminrunner

import kotlin.test.Test
import kotlin.test.assertFailsWith

class HalfConfiguredStopsTheStartupTest : BootstrapSuperAdminRunnerFixture() {
    @Test
    fun `given only the email when starting then the startup fails instead of silently skipping`() {
        assertFailsWith<IllegalStateException> { runWith(email = "root@example.com", password = "") }
    }

    @Test
    fun `given only the password when starting then the startup fails instead of silently skipping`() {
        assertFailsWith<IllegalStateException> { runWith(email = "", password = "a-long-passphrase-1") }
    }
}
