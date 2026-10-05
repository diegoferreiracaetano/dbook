package com.dbook.presentation.customerprivacy

import com.dbook.domain.identity.Role
import kotlin.test.Test
import kotlin.test.assertEquals

class AnonymizingNeedsThePermissionThePhraseAndACustomerTest : CustomerPrivacyFixture() {
    @Test
    fun `given a support member, a wrong phrase or a staff id when anonymizing then 403, 400 and 404`() {
        val id = newCustomerId()
        val staffEmail = uniqueEmail()
        registerStaff(staffEmail, Role.SUPPORT)
        val admin = superAdminToken()

        assertEquals(403, anonymize(staffToken(Role.SUPPORT), id).response.status)
        assertEquals(400, anonymize(admin, id, confirmation = "ANONYMIZE ${id + 1}").response.status)
        assertEquals(400, anonymize(admin, id, reason = "short").response.status)
        assertEquals(
            404,
            anonymize(admin, userIdOf(staffEmail), confirmation = "ANONYMIZE ${userIdOf(staffEmail)}").response.status,
        )
    }
}
