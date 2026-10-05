package com.dbook.application.payment

import com.dbook.domain.booking.BookingNotFoundException
import com.dbook.domain.booking.BookingRepository
import com.dbook.domain.booking.NotBookingOwnerException
import com.dbook.domain.identity.Actor
import com.dbook.domain.identity.Role
import com.dbook.domain.payment.Refund
import com.dbook.domain.payment.RefundReason
import io.micrometer.observation.annotation.Observed
import org.springframework.stereotype.Service

/**
 * A customer asks for the money of their own paid booking back. It is the staff's refund (the same saga, the same
 * idempotency, the same one-live-refund-per-booking rule, the same audit) with the customer as the actor, without the
 * override that lets staff refund inside the last 24 hours: outside the window the customer is told to talk to support.
 */
@Observed(name = "dbook.usecase")
@Service
class RequestOwnRefundUseCase(
    private val bookingRepository: BookingRepository,
    private val refundBookingUseCase: RefundBookingUseCase,
) {
    fun execute(
        userId: Long,
        bookingId: Long,
        idempotencyKey: String,
    ): Refund {
        val booking = bookingRepository.findById(bookingId) ?: throw BookingNotFoundException(bookingId)
        if (booking.customerId != userId) {
            throw NotBookingOwnerException(bookingId)
        }
        return refundBookingUseCase.execute(
            RefundCommand(
                Actor(userId, Role.CLIENT),
                bookingId,
                RefundReason.CUSTOMER_REQUEST,
                null,
                false,
                idempotencyKey,
            ),
        )
    }
}
