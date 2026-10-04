package com.dbook.presentation.staffmanagement

import com.dbook.domain.identity.Role
import kotlin.test.Test

class ACustomerIdIsNotFoundOnTheStaffEndpointsTest : StaffManagementFixture() {
    @Test
    fun `given a customer id when changing the role or blocking then it is 404 like any unknown id`() {
        val adminToken = newSuperAdminToken()
        val customerEmail = uniqueEmail()
        registerAndLogin(customerEmail)
        val customerId = userIdOf(customerEmail)

        changeRoleOf(adminToken, customerId, Role.SUPPORT).andExpect { status { isNotFound() } }
        blockStaff(adminToken, customerId).andExpect { status { isNotFound() } }
    }
}
