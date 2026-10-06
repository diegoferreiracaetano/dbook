package com.dbook.application.promo

import com.dbook.domain.booking.Booking
import com.dbook.domain.booking.BookingNotFoundException
import com.dbook.domain.booking.BookingRepository
import com.dbook.domain.booking.BookingStatus
import com.dbook.domain.booking.NotBookingOwnerException
import com.dbook.domain.promo.PromoCode
import com.dbook.domain.promo.PromoNotFoundException
import com.dbook.domain.promo.PromoRejectedException
import com.dbook.domain.promo.PromoRejection
import com.dbook.domain.promo.PromoRepository
import com.dbook.domain.promo.PromoType
import io.micrometer.observation.annotation.Observed
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Clock

data class ValidatePromoCommand(
    val userId: Long,
    val code: String,
    val bookingIds: List<Long>,
)

/** What the code would do to this payment, for the checkout screen to show before the customer pays. */
data class PromoPreview(
    val code: String,
    val type: PromoType,
    val subtotal: BigDecimal,
    val discount: BigDecimal,
    val total: BigDecimal,
)

/**
 * A preview of a code on the caller's own PENDING bookings: the same rules as paying, **without using the code up**.
 * It cannot promise the code will still be there at payment (someone may take the last use in between): that is
 * decided, atomically, when the payment is made.
 */
@Observed(name = "dbook.usecase")
@Service
class ValidatePromoUseCase(
    private val promoRepository: PromoRepository,
    private val bookingRepository: BookingRepository,
    private val clock: Clock,
) {
    @Transactional(readOnly = true)
    fun execute(command: ValidatePromoCommand): PromoPreview {
        require(command.bookingIds.isNotEmpty()) { "bookingIds must not be empty" }
        val promo =
            promoRepository.findByCode(PromoCode.normalize(command.code)) ?: throw PromoNotFoundException(command.code)
        val bookings = command.bookingIds.map { pendingBookingOf(command.userId, it) }
        val subtotal = bookings.fold(BigDecimal.ZERO) { total, booking -> total + booking.price }
        val discount = promo.discountFor(subtotal, clock.instant(), bookings.size)
        availabilityRejection(promo, command.userId)?.let { throw PromoRejectedException(promo.code, it) }
        return PromoPreview(promo.code, promo.type, subtotal, discount, subtotal - discount)
    }

    private fun pendingBookingOf(
        userId: Long,
        id: Long,
    ): Booking {
        val booking = bookingRepository.findById(id) ?: throw BookingNotFoundException(id)
        if (booking.customerId != userId) throw NotBookingOwnerException(id)
        check(booking.status == BookingStatus.PENDING) { "Booking $id is not PENDING" }
        return booking
    }

    private fun availabilityRejection(
        promo: PromoCode,
        userId: Long,
    ): PromoRejection? =
        when {
            promo.isExhausted -> PromoRejection.EXHAUSTED
            promoRepository.redemptionsBy(requireNotNull(promo.id), userId) >= promo.maxPerUser ->
                PromoRejection.USER_LIMIT_REACHED
            else -> null
        }
}
