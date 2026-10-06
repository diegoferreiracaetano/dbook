package com.dbook.domain.booking

import com.dbook.domain.catalog.Bookable
import java.math.BigDecimal

/**
 * A reservation of a [Bookable] by a customer. Starts [BookingStatus.PENDING] and can
 * only move forward once — [confirm] and [cancel] both reject a booking that has
 * already left the PENDING state (see [transitionTo]).
 */
class Booking(
    val id: Long? = null,
    val bookable: Bookable,
    val seatId: Long?,
    val customerId: Long,
    val status: BookingStatus = BookingStatus.PENDING,
    val paymentId: Long? = null,
    val version: Long = 0,
    val price: BigDecimal = bookable.price,
    val discount: BigDecimal = BigDecimal("0.00"),
    val stay: Stay? = null,
) {
    init {
        require((seatId == null) != (stay == null)) { "a booking is of a seat or of a stay, not both and not neither" }
        require(price >= BigDecimal.ZERO) { "price must not be negative" }
        require(discount >= BigDecimal.ZERO && discount <= price) { "discount must be between zero and the price" }
    }

    /** What was really paid for this booking: its frozen price minus its share of the payment's discount. */
    val paidAmount: BigDecimal get() = price - discount

    /** @throws IllegalStateException if this booking isn't PENDING. */
    fun confirm(
        paymentId: Long,
        discount: BigDecimal = BigDecimal.ZERO,
    ): Booking = transitionTo(BookingStatus.CONFIRMED, paymentId, discount)

    /** @throws IllegalStateException if this booking isn't PENDING. */
    fun cancel(): Booking = transitionTo(BookingStatus.CANCELLED)

    /** The money went back: a CONFIRMED booking ends here and cannot be paid, cancelled or refunded again. */
    fun refund(): Booking {
        check(status == BookingStatus.CONFIRMED) { "Only a CONFIRMED booking can be refunded" }
        return Booking(
            id, bookable, seatId, customerId, BookingStatus.REFUNDED, paymentId, version, price, discount, stay,
        )
    }

    private fun transitionTo(
        newStatus: BookingStatus,
        paymentId: Long? = this.paymentId,
        discount: BigDecimal = this.discount,
    ): Booking {
        check(status == BookingStatus.PENDING) { "Only a PENDING booking can transition to $newStatus" }
        return Booking(id, bookable, seatId, customerId, newStatus, paymentId, version, price, discount, stay)
    }
}
