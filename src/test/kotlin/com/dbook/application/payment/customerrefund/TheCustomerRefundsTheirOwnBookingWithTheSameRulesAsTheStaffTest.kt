package com.dbook.application.payment.customerrefund

import com.dbook.domain.booking.BookingStatus
import com.dbook.domain.booking.NotBookingOwnerException
import com.dbook.domain.common.access.Role
import com.dbook.domain.common.audit.AuditAction
import com.dbook.domain.payment.RefundReason
import com.dbook.domain.payment.RefundStatus
import com.dbook.domain.payment.RefundWindowClosedException
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TheCustomerRefundsTheirOwnBookingWithTheSameRulesAsTheStaffTest : OwnRefundFixture() {
    @Test
    fun `given a paid booking when the customer requests a refund then it completes, with them as the actor`() {
        val refund = committed { requestOwnRefund.execute(owner, 100, "key-1") }

        assertEquals(RefundStatus.COMPLETED, refund.status)
        assertEquals(BigDecimal("500.00"), refund.amount)
        assertEquals(RefundReason.CUSTOMER_REQUEST, refund.reason)
        assertEquals(owner, refund.requestedBy)
        assertEquals(BookingStatus.REFUNDED, bookings.findById(100)!!.status)
        assertEquals(listOf(AuditAction.REFUND_REQUESTED, AuditAction.REFUND_COMPLETED), audit.events.map { it.action })
        assertEquals(Role.CLIENT, audit.events.first().actor.role)
    }

    @Test
    fun `given a discounted booking when the customer requests a refund then what was paid goes back`() {
        assertEquals(BigDecimal("460.00"), committed { requestOwnRefund.execute(owner, 103, "key-1") }.amount)
    }

    @Test
    fun `given the same request twice when repeated then it is one refund and one call to the gateway`() {
        val first = committed { requestOwnRefund.execute(owner, 100, "key-1") }
        val again = committed { requestOwnRefund.execute(owner, 100, "key-1") }

        assertEquals(first.id, again.id)
        assertEquals(1, gateway.calls.size)
    }

    @Test
    fun `given someone else's booking when requested then it is refused and nothing moves`() {
        assertFailsWith<NotBookingOwnerException> { committed { requestOwnRefund.execute(stranger, 100, "key-1") } }

        assertEquals(0, gateway.calls.size)
        assertEquals(BookingStatus.CONFIRMED, bookings.findById(100)!!.status)
    }

    @Test
    fun `given a booking inside the last day when the customer requests then it is refused, they have no override`() {
        assertFailsWith<RefundWindowClosedException> { committed { requestOwnRefund.execute(owner, 101, "key-1") } }

        assertEquals(BookingStatus.CONFIRMED, bookings.findById(101)!!.status)
    }

    @Test
    fun `given an unpaid booking when the customer requests a refund then it is refused`() {
        assertFailsWith<IllegalStateException> { committed { requestOwnRefund.execute(owner, 102, "key-1") } }
    }

    @Test
    fun `given a gateway that refuses when requested then the refund is FAILED and the booking stays paid`() {
        gateway.failure = "gateway refuses"

        val refund = committed { requestOwnRefund.execute(owner, 100, "key-1") }

        assertEquals(RefundStatus.FAILED, refund.status)
        assertEquals(BookingStatus.CONFIRMED, bookings.findById(100)!!.status)
    }
}
