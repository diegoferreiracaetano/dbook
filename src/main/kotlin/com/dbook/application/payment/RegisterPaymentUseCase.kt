package com.dbook.application.payment

import com.dbook.application.common.afterCommit
import com.dbook.application.common.countOutcome
import com.dbook.domain.booking.BookingEvents
import com.dbook.domain.booking.BookingNotFoundException
import com.dbook.domain.booking.BookingRepository
import com.dbook.domain.booking.NotBookingOwnerException
import com.dbook.domain.messaging.OutboxWriter
import com.dbook.domain.payment.Payment
import com.dbook.domain.payment.PaymentRepository
import com.dbook.domain.promo.PromoCode
import com.dbook.domain.promo.PromoNotFoundException
import com.dbook.domain.promo.PromoRejectedException
import com.dbook.domain.promo.PromoRejection
import com.dbook.domain.promo.PromoRepository
import com.dbook.domain.promo.RedeemResult
import com.dbook.domain.promo.allocateDiscount
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.observation.annotation.Observed
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Clock

data class RegisterPaymentCommand(
    val bookingIds: List<Long>,
    val cardLast4: String,
    val cardholderName: String,
    val requestingUserId: Long,
    val idempotencyKey: String,
    val promoCode: String? = null,
) {
    // The code is part of the request: the same key with another code is another request. Without a code the
    // fingerprint is what it always was, so payments made before codes existed still replay.
    fun fingerprint(): String {
        val bookings = bookingIds.sorted().joinToString(",")
        return promoCode?.let { fingerprintOf(bookings, cardLast4, cardholderName, PromoCode.normalize(it)) }
            ?: fingerprintOf(bookings, cardLast4, cardholderName)
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
    private val meterRegistry: MeterRegistry,
    private val outboxWriter: OutboxWriter,
    private val promoRepository: PromoRepository,
    private val clock: Clock,
) {
    @Transactional
    fun execute(command: RegisterPaymentCommand): Payment {
        val fingerprint = command.fingerprint()
        return idempotently(
            previous =
                paymentRepository.findByCustomerIdAndIdempotencyKey(command.requestingUserId, command.idempotencyKey),
            previousFingerprint = { it.requestFingerprint },
            fingerprint = fingerprint,
            onReplay = { meterRegistry.countOutcome("dbook.payment", "replayed") },
        ) { pay(command, fingerprint) }
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

        val subtotal = bookings.fold(BigDecimal.ZERO) { total, booking -> total + booking.price }
        val promo = command.promoCode?.let { findPromo(it) }
        val discount = promo?.discountFor(subtotal, clock.instant(), bookings.size) ?: BigDecimal("0.00")
        val payment =
            paymentRepository.save(
                Payment(
                    customerId = command.requestingUserId,
                    amount = subtotal - discount,
                    cardLast4 = command.cardLast4,
                    cardholderName = command.cardholderName,
                    idempotencyKey = command.idempotencyKey,
                    requestFingerprint = fingerprint,
                    subtotal = subtotal,
                    discount = discount,
                    promoCodeId = promo?.id,
                    promoCode = promo?.code,
                ),
            )

        val paymentId = requireNotNull(payment.id) { "A saved Payment must have an id" }
        promo?.let { redeem(it, command.requestingUserId, paymentId, discount) }
        val shares = allocateDiscount(discount, bookings.map { it.price })
        bookings.zip(shares).forEach { (booking, share) ->
            val confirmed = bookingRepository.save(booking.confirm(paymentId, share))
            // the notification of the payment is an outbox event, in the transaction of the payment itself
            outboxWriter.add(BookingEvents.confirmed(confirmed, clock.instant()))
        }

        // after the commit, not here: a payment that is rolled back (e.g. it lost the race against a
        // cancellation) never happened, so it must not be counted as created
        afterCommit { meterRegistry.countOutcome("dbook.payment", "created") }

        return payment
    }

    private fun findPromo(code: String): PromoCode =
        promoRepository.findByCode(PromoCode.normalize(code)) ?: throw PromoNotFoundException(code)

    // Throwing undoes the payment and the use together: a code that lost the race leaves no trace
    private fun redeem(
        promo: PromoCode,
        userId: Long,
        paymentId: Long,
        discount: BigDecimal,
    ) {
        val rejection =
            when (promoRepository.redeem(requireNotNull(promo.id), userId, paymentId, discount, clock.instant())) {
                RedeemResult.REDEEMED -> null
                RedeemResult.EXHAUSTED -> PromoRejection.EXHAUSTED
                RedeemResult.USER_LIMIT_REACHED -> PromoRejection.USER_LIMIT_REACHED
            }
        rejection?.let { throw PromoRejectedException(promo.code, it) }
    }
}
