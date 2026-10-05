package com.dbook.domain.booking

/**
 * A [com.dbook.domain.booking.Booking] leaves PENDING once (CONFIRMED by a payment, or CANCELLED), and a CONFIRMED
 * one can still be REFUNDED, which is terminal — see [com.dbook.domain.booking.Booking.refund].
 */
enum class BookingStatus {
    PENDING,
    CONFIRMED,
    CANCELLED,
    REFUNDED,
}
