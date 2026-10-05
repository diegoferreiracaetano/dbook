package com.dbook.presentation.catalogadmin

import com.dbook.domain.identity.Role
import kotlin.test.Test
import kotlin.test.assertEquals

class OnlyFlightWriteCanImportTest : FlightImportFixture() {
    @Test
    fun `given support or a customer when importing then 403`() {
        val csv = "$header\n"

        assertEquals(403, import(registerStaffAndLogin(uniqueEmail(), Role.SUPPORT), csv).response.status)
        assertEquals(403, import(registerAndLogin(uniqueEmail()), csv).response.status)
    }
}
