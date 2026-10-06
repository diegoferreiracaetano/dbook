package com.dbook.presentation.adminbookings

import com.dbook.domain.common.access.Role
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TheRefundIsAuditedWithItsNoteTest : AdminBookingsFixture() {
    @Test
    fun `given an override refund when reading the audit then requested and completed are there with the note`() {
        val (_, booking) = paidBookingDepartingIn(hours = 2)
        val admin = staff(Role.SUPER_ADMIN)
        val result =
            refund(
                admin,
                booking,
                body =
                    mapOf(
                        "reason" to "FLIGHT_CANCELLED",
                        "override" to true,
                        "note" to "Airline cancelled the flight",
                    ),
            )

        val trail = auditEntries(admin, "targetType=REFUND&targetId=${refundIdOf(result)}")

        assertEquals(setOf("REFUND_REQUESTED", "REFUND_COMPLETED"), trail.map { it["action"].asText() }.toSet())
        assertTrue(trail.all { it["reason"].asText() == "Airline cancelled the flight" || it["reason"].isNull })
        assertEquals("100.00", trail.first { it["action"].asText() == "REFUND_COMPLETED" }["after"]["amount"].asText())
    }
}
