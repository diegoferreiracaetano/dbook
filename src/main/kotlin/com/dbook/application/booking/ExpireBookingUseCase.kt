package com.dbook.application.booking

import com.dbook.domain.booking.BookingRepository
import com.dbook.domain.booking.BookingStatus
import com.dbook.domain.identity.Role
import org.springframework.stereotype.Service

/**
 * Cancels a booking that stayed PENDING past its time limit, freeing its seat. Idempotent on
 * purpose: SQS delivers at least once, and the booking may have been paid (or cancelled) in
 * the meantime — in both cases there is nothing left to expire and this simply returns.
 */
@Service
class ExpireBookingUseCase(
    private val bookingRepository: BookingRepository,
    private val cancelBookingUseCase: CancelBookingUseCase,
) {
    fun execute(bookingId: Long) {
        val booking = bookingRepository.findById(bookingId) ?: return
        if (booking.status != BookingStatus.PENDING) return

        // Cancels on behalf of the booking's own owner, which passes the ownership check
        // without needing ADMIN. If a payment commits between the check above and this call,
        // the cancel fails and the message is redelivered — the next attempt finds it paid.
        cancelBookingUseCase.execute(bookingId, booking.customerId, Role.CLIENT)
    }
}
