package com.dbook.domain

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
) {
    /** @throws IllegalStateException if this booking isn't PENDING. */
    fun confirm(paymentId: Long): Booking = transitionTo(BookingStatus.CONFIRMED, paymentId)

    /** @throws IllegalStateException if this booking isn't PENDING. */
    fun cancel(): Booking = transitionTo(BookingStatus.CANCELLED)

    private fun transitionTo(
        newStatus: BookingStatus,
        paymentId: Long? = this.paymentId,
    ): Booking {
        check(status == BookingStatus.PENDING) { "Only a PENDING booking can transition to $newStatus" }
        return Booking(id, bookable, seatId, customerId, newStatus, paymentId)
    }
}
