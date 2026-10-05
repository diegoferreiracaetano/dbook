package com.dbook.presentation.customernotes

import kotlin.test.Test
import kotlin.test.assertEquals

class BlockingACustomerClosesTheirAccessAndUnblockingReopensItTest : CustomerNotesFixture() {
    @Test
    fun `given a customer with a session when blocked then login is 403, the session ends, unblocked it works`() {
        val email = uniqueEmail()
        registerAndLogin(email)
        val refresh = loginRefreshToken(email, "s3cret-password")
        val id = userIdOf(email)
        val support = staffToken()

        val blocked = blockCustomer(support, id, "Chargeback fraud confirmed")

        assertEquals(200, blocked.response.status)
        assertEquals("BLOCKED", bodyOf(blocked)["status"].asText())
        assertEquals(403, loginStatus(email))
        assertEquals(401, refreshStatus(refresh))
        assertEquals(200, unblockCustomer(support, id).response.status)
        assertEquals(200, loginStatus(email))
    }
}
