package com.dbook.application

import com.dbook.domain.BookingNotFoundException
import com.dbook.domain.BookingRepository
import com.dbook.domain.NotBookingOwnerException
import com.dbook.domain.Payment
import com.dbook.domain.PaymentRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

data class RegisterPaymentCommand(
    val bookingIds: List<Long>,
    val cardLast4: String,
    val cardholderName: String,
    val requestingUserId: Long,
)

/**
 * Pays for one or more PENDING [com.dbook.domain.Booking]s in a single transaction —
 * covers a whole Round Trip (outbound + return) at once, not one payment per leg.
 * Confirms every booking (see [com.dbook.domain.Booking.confirm]) only after the
 * [Payment] itself is persisted, so a confirmed booking always points at a real payment.
 */
@Service
class RegisterPaymentUseCase(
    private val bookingRepository: BookingRepository,
    private val paymentRepository: PaymentRepository,
) {
    @Transactional
    fun execute(command: RegisterPaymentCommand): Payment {
        val bookings =
            command.bookingIds.map { bookingId ->
                val booking =
                    bookingRepository.findById(bookingId)
                        ?: throw BookingNotFoundException(bookingId)
                if (booking.customerId != command.requestingUserId) {
                    throw NotBookingOwnerException(bookingId)
                }
                booking
            }

        val amount = bookings.fold(BigDecimal.ZERO) { total, booking -> total + booking.bookable.price }
        val payment =
            paymentRepository.save(
                Payment(
                    customerId = command.requestingUserId,
                    amount = amount,
                    cardLast4 = command.cardLast4,
                    cardholderName = command.cardholderName,
                ),
            )

        val paymentId = requireNotNull(payment.id) { "A saved Payment must have an id" }
        bookings.forEach { booking -> bookingRepository.save(booking.confirm(paymentId)) }

        return payment
    }
}
