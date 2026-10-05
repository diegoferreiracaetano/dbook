package com.dbook.presentation.customerprivacy

import kotlin.test.Test
import kotlin.test.assertEquals

class ACustomerCanExportAndThenDeleteTheirOwnDataTest : CustomerPrivacyFixture() {
    @Test
    fun `given a customer when exporting their data and deleting the account then data comes out and access ends`() {
        val email = uniqueEmail()
        val token = registerAndLogin(email, name = "Maria Silva")
        bookSeats(token, 2)

        val export = bodyOf(myExport(token))

        assertEquals(email, export["profile"]["email"].asText())
        assertEquals("Maria Silva", export["profile"]["name"].asText())
        assertEquals(2, export["bookings"].size())
        assertEquals(401, deleteMyAccount(token, "not-my-password").response.status)
        assertEquals(200, loginStatus(email))
        assertEquals(204, deleteMyAccount(token, "s3cret-password").response.status)
        assertEquals(401, loginStatus(email))
        assertEquals(409, registerStatus(email))
    }
}
