package com.dbook.domain.booking

/**
 * PENDING is the only state a [com.dbook.domain.booking.Booking] can transition out of — see
 * [com.dbook.domain.booking.Booking.confirm]/[com.dbook.domain.booking.Booking.cancel].
 */
enum class BookingStatus {
    PENDING,
    CONFIRMED,
    CANCELLED,
}
