package com.dbook.application.payment.customerrefund

import com.dbook.application.payment.GetCancellationPolicyUseCase
import com.dbook.application.payment.RequestOwnRefundUseCase
import com.dbook.application.payment.refund.RefundUseCaseFixture

// Customer 3 owns bookings 100 (paid, 16 days away), 101 (paid, leaves in 6 hours), 102 (pending) and 103 (paid with a
// 40.00 discount); customer 9 owns none.
abstract class OwnRefundFixture : RefundUseCaseFixture() {
    protected val owner = 3L
    protected val stranger = 9L
    protected val policy = GetCancellationPolicyUseCase(bookings, refunds, clock)
    protected val requestOwnRefund = RequestOwnRefundUseCase(bookings, refundBooking)
}
