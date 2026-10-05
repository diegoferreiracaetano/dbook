package com.dbook.presentation.promo

import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class TheSameKeyWithTheSameCodeReplaysAndNeverUsesTheCodeTwiceTest : PromoApiFixture() {
    @Test
    fun `given a payment with a code when the request is repeated then the same payment returns, one use counted`() {
        val (id, code) = newPromo(manager())
        val (token, booking) = customerWithABooking()
        val key = UUID.randomUUID().toString()

        val first = payWith(token, listOf(booking), code, key)
        val retry = payWith(token, listOf(booking), code, key)

        assertEquals(201, retry.response.status)
        assertEquals(json(first)["id"].asLong(), json(retry)["id"].asLong())
        assertEquals(90.0, json(retry)["amount"].asDouble())
        assertEquals(1, redeemed(id))
    }

    @Test
    fun `given a payment with a code when the same key is used with no code then it is a 422`() {
        val (_, code) = newPromo(manager())
        val (token, booking) = customerWithABooking()
        val key = UUID.randomUUID().toString()
        payWith(token, listOf(booking), code, key)

        assertEquals(422, payWith(token, listOf(booking), null, key).response.status)
    }
}
