package com.dbook.presentation.adminbookings

import org.springframework.test.web.servlet.get
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AGatewayFailureCanBeFoundAndRetriedTest : AdminBookingsFixture() {
    @Test
    fun `given a gateway that refuses when refunding then FAILED and untouched, and a retry later completes it`() {
        val (_, booking) = paidBooking()
        val support = staff()

        gateway.failure = "processor timeout"
        val failed =
            try {
                refund(support, booking)
            } finally {
                gateway.failure = null
            }

        assertEquals(201, failed.response.status)
        assertEquals("FAILED", statusOfRefund(failed))
        assertEquals("processor timeout", bodyOf(failed)["failureReason"].asText())
        assertEquals("CONFIRMED", bodyOf(adminBooking(support, booking))["booking"]["status"].asText())
        assertEquals("RESERVED", seatStatusOf(booking))
        val listed =
            mockMvc.get("/v1/admin/refunds") {
                header("Authorization", "Bearer $support")
                param("status", "FAILED")
            }.andReturn()
        assertTrue(bodyOf(listed)["items"].any { it["id"].asLong() == refundIdOf(failed) })

        val retried = retry(support, refundIdOf(failed))

        assertEquals("COMPLETED", statusOfRefund(retried))
        assertEquals("REFUNDED", bodyOf(adminBooking(support, booking))["booking"]["status"].asText())
        assertEquals(gateway.calls.takeLast(2).map { it.idempotencyKey }.toSet().size, 1)
    }
}
