package com.dbook.presentation.customerhistory

import com.dbook.domain.identity.Role
import kotlin.test.Test
import kotlin.test.assertEquals

class OnlyWhoCanReadCustomersCanOpenTheHistoryTest : CustomerHistoryFixture() {
    @Test
    fun `given a catalog manager when listing the history then it is 403 on every list, and support gets 200`() {
        val email = uniqueEmail()
        registerAndLogin(email)
        val customerId = userIdOf(email)
        val catalogToken = registerStaffAndLogin(uniqueEmail(), Role.CATALOG_MANAGER)
        val supportToken = supportToken()

        listOf("bookings", "payments", "reviews").forEach { list ->
            assertEquals(403, history(catalogToken, customerId, list).response.status, list)
            assertEquals(200, history(supportToken, customerId, list).response.status, list)
        }
    }
}
