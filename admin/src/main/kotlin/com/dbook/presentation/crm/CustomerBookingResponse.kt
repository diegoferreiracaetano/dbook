package com.dbook.presentation.crm

import com.dbook.domain.crm.CustomerBooking
import java.math.BigDecimal
import java.time.LocalDateTime

/** `price` is the one frozen when the booking was made, not the flight's current price. */
data class CustomerBookingResponse(
    val id: Long,
    val status: String,
    val price: BigDecimal,
    val title: String,
    val seatLabel: String?,
    val flightNumber: String?,
    val origin: String?,
    val destination: String?,
    val departureTime: LocalDateTime?,
    val paymentId: Long?,
) {
    companion object {
        fun from(booking: CustomerBooking) =
            CustomerBookingResponse(
                id = booking.id,
                status = booking.status,
                price = booking.price,
                title = booking.title,
                seatLabel = booking.seatLabel,
                flightNumber = booking.flightNumber,
                origin = booking.origin,
                destination = booking.destination,
                departureTime = booking.departureTime,
                paymentId = booking.paymentId,
            )
    }
}
