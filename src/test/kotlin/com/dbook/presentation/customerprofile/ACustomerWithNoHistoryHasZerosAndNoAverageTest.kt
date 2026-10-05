package com.dbook.presentation.customerprofile

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ACustomerWithNoHistoryHasZerosAndNoAverageTest : CustomerProfileFixture() {
    @Test
    fun `given a customer without history when reading the profile then totals are zero and no average`() {
        val email = uniqueEmail()
        registerAndLogin(email)

        val body = bodyOf(profile(supportToken(), userIdOf(email)))

        assertEquals(0, body["bookings"]["total"].asInt())
        assertEquals(0.0, body["payments"]["totalPaid"].asDouble())
        assertTrue(body["reviews"]["averageRating"].isNull)
    }
}
