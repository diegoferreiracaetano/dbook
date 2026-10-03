package com.dbook.application.payment

import com.dbook.domain.booking.BookingNotFoundException
import com.dbook.domain.booking.BookingRepository
import com.dbook.domain.booking.NotBookingOwnerException
import com.dbook.domain.payment.IdempotencyKeyReusedException
import com.dbook.domain.payment.Payment
import com.dbook.domain.payment.PaymentRepository
import io.micrometer.observation.annotation.Observed
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.security.MessageDigest
import java.util.HexFormat

data class RegisterPaymentCommand(
    val bookingIds: List<Long>,
    val cardLast4: String,
    val cardholderName: String,
    val requestingUserId: Long,
    val idempotencyKey: String,
) {
    fun fingerprint(): String {
        val canonical = "${bookingIds.sorted().joinToString(",")}|$cardLast4|$cardholderName"
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical.toByteArray()))
    }
}

/**
 * Pays for one or more PENDING [com.dbook.domain.booking.Booking]s in a single transaction —
 * covers a whole Round Trip (outbound + return) at once, not one payment per leg.
 * Confirms every booking (see [com.dbook.domain.booking.Booking.confirm]) only after the
 * [Payment] itself is persisted, so a confirmed booking always points at a real payment.
 */
@Observed(name = "dbook.usecase")
@Service
class RegisterPaymentUseCase(
    private val bookingRepository: BookingRepository,
    private val paymentRepository: PaymentRepository,
) {
    @Transactional
    fun execute(command: RegisterPaymentCommand): Payment {
        val fingerprint = command.fingerprint()
        val previous =
            paymentRepository.findByCustomerIdAndIdempotencyKey(command.requestingUserId, command.idempotencyKey)
        return if (previous == null) pay(command, fingerprint) else replay(previous, fingerprint)
    }

    private fun replay(
        previous: Payment,
        fingerprint: String,
    ): Payment {
        if (previous.requestFingerprint != fingerprint) {
            throw IdempotencyKeyReusedException()
        }
        return previous
    }

    private fun pay(
        command: RegisterPaymentCommand,
        fingerprint: String,
    ): Payment {
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
                    idempotencyKey = command.idempotencyKey,
                    requestFingerprint = fingerprint,
                ),
            )

        val paymentId = requireNotNull(payment.id) { "A saved Payment must have an id" }
        bookings.forEach { booking -> bookingRepository.save(booking.confirm(paymentId)) }

        return payment
    }
}
