package com.dbook.domain

/**
 * PENDING is the only state a [com.dbook.domain.Booking] can transition out of — see
 * [com.dbook.domain.Booking.confirm]/[com.dbook.domain.Booking.cancel].
 */
enum class BookingStatus {
    PENDING,
    CONFIRMED,
    CANCELLED,
}
