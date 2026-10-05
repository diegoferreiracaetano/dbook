package com.dbook.application.payment.refund

import com.dbook.domain.booking.BookingStatus
import com.dbook.domain.payment.RefundOverrideNotAllowedException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class OnlyASuperAdminCanOverrideTheWindowAndNeedsANoteTest : RefundUseCaseFixture() {
    @Test
    fun `given a flight in 6 hours when overriding then support is refused, a note is needed, a SUPER_ADMIN works`() {
        assertFailsWith<RefundOverrideNotAllowedException> {
            committed { refundBooking.execute(command(101, actor = support, note = "why", override = true)) }
        }
        assertFailsWith<IllegalArgumentException> {
            committed { refundBooking.execute(command(101, actor = superAdmin, note = "  ", override = true)) }
        }

        committed {
            refundBooking.execute(
                command(101, actor = superAdmin, note = "Airline cancelled the flight", override = true),
            )
        }

        assertEquals(BookingStatus.REFUNDED, statusOf(101))
        assertEquals("Airline cancelled the flight", audit.events.first().reason)
    }
}
