package com.dbook.infrastructure.messaging

data class BookingExpirationMessage(
    val bookingId: Long,
)
