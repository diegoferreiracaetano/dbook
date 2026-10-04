package com.dbook.presentation.identity.bootstrapsuperadminrunner

import kotlin.test.Test
import kotlin.test.assertTrue

class CreatesTheSuperAdminWhenBothAreSetTest : BootstrapSuperAdminRunnerFixture() {
    @Test
    fun `given both settings when starting then the SUPER_ADMIN is created, and starting again is harmless`() {
        runWith(email = "root@example.com", password = "a-long-passphrase-1")
        runWith(email = "root@example.com", password = "a-long-passphrase-1")

        assertTrue(hasSuperAdmin())
    }
}
