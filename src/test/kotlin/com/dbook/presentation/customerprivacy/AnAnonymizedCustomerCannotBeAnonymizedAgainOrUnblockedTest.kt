package com.dbook.presentation.customerprivacy

import kotlin.test.Test
import kotlin.test.assertEquals

class AnAnonymizedCustomerCannotBeAnonymizedAgainOrUnblockedTest : CustomerPrivacyFixture() {
    @Test
    fun `given an anonymized customer when anonymizing again or unblocking then both are 409`() {
        val id = newCustomerId()
        val admin = superAdminToken()
        anonymize(admin, id)

        assertEquals(409, anonymize(admin, id).response.status)
        assertEquals(409, unblockCustomer(admin, id).response.status)
    }
}
