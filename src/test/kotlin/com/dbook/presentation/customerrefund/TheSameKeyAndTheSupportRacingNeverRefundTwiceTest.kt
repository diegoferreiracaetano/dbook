package com.dbook.presentation.customerrefund

import java.util.UUID
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import kotlin.test.Test
import kotlin.test.assertEquals

class TheSameKeyAndTheSupportRacingNeverRefundTwiceTest : CustomerRefundFixture() {
    @Test
    fun `given the same request sent twice when repeated then the same refund returns and the gateway is asked once`() {
        val (token, booking) = paidBooking()
        val key = UUID.randomUUID().toString()
        val before = gateway.calls.size

        val first = refundRequest(token, booking, key)
        val retry = refundRequest(token, booking, key)

        assertEquals(201, retry.response.status)
        assertEquals(bodyOf(first)["id"].asLong(), bodyOf(retry)["id"].asLong())
        assertEquals(before + 1, gateway.calls.size)
        assertEquals(1, refundRowsOf(booking))
    }

    @Test
    fun `given a key already used for another booking when it is reused then it is a 422`() {
        val token = registerAndLogin(uniqueEmail())
        val (first, other) = bookSeats(token, 2)
        pay(token, first)
        pay(token, other)
        val key = UUID.randomUUID().toString()
        refundRequest(token, first, key)

        assertEquals(422, refundRequest(token, other, key).response.status)
    }

    @Test
    fun `given the customer and the support refunding the same booking at once then exactly one refund exists`() {
        val (token, booking) = paidBooking()
        val support = staff()
        val before = gateway.calls.size
        val barrier = CyclicBarrier(2)
        val pool = Executors.newFixedThreadPool(2)

        val statuses =
            try {
                listOf({ refundRequest(token, booking) }, { refund(support, booking) }).map { call ->
                    pool.submit<Int> {
                        barrier.await()
                        call().response.status
                    }
                }.map { it.get() }
            } finally {
                pool.shutdown()
            }

        assertEquals(listOf(201, 409), statuses.sorted())
        assertEquals(1, refundRowsOf(booking))
        assertEquals(before + 1, gateway.calls.size)
        assertEquals("REFUNDED", bodyOf(adminBooking(support, booking))["booking"]["status"].asText())
    }
}
