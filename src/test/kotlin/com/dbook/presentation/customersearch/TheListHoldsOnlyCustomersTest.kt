package com.dbook.presentation.customersearch

import com.dbook.domain.common.access.Role
import kotlin.test.Test
import kotlin.test.assertEquals

class TheListHoldsOnlyCustomersTest : CustomerSearchFixture() {
    @Test
    fun `given a customer and a staff member with the same tag when searching then only the customer is listed`() {
        val tag = newTag()
        newCustomer("Customer $tag")
        val staffEmail = uniqueEmail()
        registerAndLogin(staffEmail, name = "Staff $tag")
        changeRole(staffEmail, Role.SUPPORT)

        val result = search(supportToken(), "query" to tag)

        assertEquals(listOf("Customer $tag"), namesOf(result))
    }
}
