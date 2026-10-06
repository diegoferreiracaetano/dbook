package com.dbook.application.payment.refund

import kotlin.test.Test
import kotlin.test.assertEquals

class CompletingTwiceReleasesTheSeatOnlyOnceTest : RefundUseCaseFixture() {
    @Test
    fun `given a completed refund when it is settled again then nothing happens twice`() {
        val refund = committed { refundBooking.execute(command()) }

        committed { settler.complete(requireNotNull(refund.id), support) }

        assertEquals(listOf(1000L), seats.released)
        assertEquals(1, audit.events.count { it.action == com.dbook.domain.common.audit.AuditAction.REFUND_COMPLETED })
    }
}
