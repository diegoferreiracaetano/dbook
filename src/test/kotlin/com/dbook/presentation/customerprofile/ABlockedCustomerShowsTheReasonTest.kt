package com.dbook.presentation.customerprofile

import kotlin.test.Test
import kotlin.test.assertEquals

class ABlockedCustomerShowsTheReasonTest : CustomerProfileFixture() {
    @Test
    fun `given a blocked customer when reading the profile then the status and the reason are there`() {
        val email = uniqueEmail()
        registerAndLogin(email)
        block(email, "chargeback fraud")

        val body = bodyOf(profile(supportToken(), userIdOf(email)))

        assertEquals("BLOCKED", body["status"].asText())
        assertEquals("chargeback fraud", body["blockedReason"].asText())
    }
}
