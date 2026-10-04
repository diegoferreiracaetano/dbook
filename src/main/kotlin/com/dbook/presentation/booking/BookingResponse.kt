package com.dbook.presentation.booking

import com.dbook.domain.booking.Booking
import com.dbook.domain.booking.BookingStatus
import java.math.BigDecimal

data class BookingResponse(
    val id: Long?,
    val bookableId: Long?,
    val seatId: Long,
    val customerId: Long,
    val status: BookingStatus,
    val price: BigDecimal,
) {
    companion object {
        fun from(booking: Booking) =
            BookingResponse(
                id = booking.id,
                bookableId = booking.bookable.id,
                seatId = booking.seatId,
                customerId = booking.customerId,
                status = booking.status,
                price = booking.price,
            )
    }
}
