package com.dbook.presentation.customernotes

import com.dbook.domain.common.access.Role
import kotlin.test.Test
import kotlin.test.assertEquals

class BlockingNeedsAReasonOfTenCharactersTheRightPermissionAndACustomerTest : CustomerNotesFixture() {
    @Test
    fun `given a short reason, a catalog manager or a staff id when blocking then 400, 403 and 404`() {
        val id = newCustomerId()
        val staffEmail = uniqueEmail()
        registerStaff(staffEmail, Role.SUPPORT)

        assertEquals(400, blockCustomer(staffToken(), id, "too short").response.status)
        assertEquals(403, blockCustomer(staffToken(Role.CATALOG_MANAGER), id, "a reason long enough").response.status)
        assertEquals(404, blockCustomer(staffToken(), userIdOf(staffEmail), "a reason long enough").response.status)
    }
}
