package com.dbook.presentation.customerprivacy

import com.dbook.domain.identity.Role
import kotlin.test.Test
import kotlin.test.assertEquals

class AStaffMemberCannotDeleteTheirAccountOverHttpTest : CustomerPrivacyFixture() {
    @Test
    fun `given a staff member when deleting their own account then it is 409 and they still log in`() {
        val email = uniqueEmail()
        val token = registerStaffAndLogin(email, Role.SUPPORT)

        assertEquals(409, deleteMyAccount(token, "s3cret-password").response.status)
        assertEquals(200, adminLogin(email).response.status)
    }
}
