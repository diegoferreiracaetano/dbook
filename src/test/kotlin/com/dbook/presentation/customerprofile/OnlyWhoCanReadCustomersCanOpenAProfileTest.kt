package com.dbook.presentation.customerprofile

import com.dbook.domain.common.access.Role
import kotlin.test.Test
import kotlin.test.assertEquals

class OnlyWhoCanReadCustomersCanOpenAProfileTest : CustomerProfileFixture() {
    @Test
    fun `given a catalog manager when opening a profile then it is 403, and a support member gets 200`() {
        val email = uniqueEmail()
        registerAndLogin(email)
        val customerId = userIdOf(email)
        val catalogToken = registerStaffAndLogin(uniqueEmail(), Role.CATALOG_MANAGER)

        assertEquals(403, profile(catalogToken, customerId).response.status)
        assertEquals(200, profile(supportToken(), customerId).response.status)
    }
}
