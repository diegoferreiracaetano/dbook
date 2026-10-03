package com.dbook.infrastructure.messaging.expiration

data class BookingExpirationMessage(
    val bookingId: Long,
)
