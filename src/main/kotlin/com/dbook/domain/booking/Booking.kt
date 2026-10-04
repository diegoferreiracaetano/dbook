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
    val seatId: Long,
    val customerId: Long,
    val status: BookingStatus = BookingStatus.PENDING,
    val paymentId: Long? = null,
    val version: Long = 0,
    val price: BigDecimal = bookable.price,
) {
    init {
        require(price >= BigDecimal.ZERO) { "price must not be negative" }
    }

    /** @throws IllegalStateException if this booking isn't PENDING. */
    fun confirm(paymentId: Long): Booking = transitionTo(BookingStatus.CONFIRMED, paymentId)

    /** @throws IllegalStateException if this booking isn't PENDING. */
    fun cancel(): Booking = transitionTo(BookingStatus.CANCELLED)

    private fun transitionTo(
        newStatus: BookingStatus,
        paymentId: Long? = this.paymentId,
    ): Booking {
        check(status == BookingStatus.PENDING) { "Only a PENDING booking can transition to $newStatus" }
        return Booking(id, bookable, seatId, customerId, newStatus, paymentId, version, price)
    }
}
