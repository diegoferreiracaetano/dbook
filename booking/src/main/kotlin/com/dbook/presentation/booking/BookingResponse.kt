package com.dbook.presentation.booking

import com.dbook.domain.booking.Booking
import com.dbook.domain.booking.BookingStatus
import java.math.BigDecimal

data class BookingResponse(
    val id: Long?,
    val bookableId: Long?,
    val seatId: Long?,
    val stay: StayResponse?,
    val customerId: Long,
    val status: BookingStatus,
    val price: BigDecimal,
    val discount: BigDecimal,
    val paidAmount: BigDecimal,
) {
    companion object {
        fun from(booking: Booking) =
            BookingResponse(
                id = booking.id,
                bookableId = booking.bookable.id,
                seatId = booking.seatId,
                stay = booking.stay?.let(StayResponse::from),
                customerId = booking.customerId,
                status = booking.status,
                price = booking.price,
                discount = booking.discount,
                paidAmount = booking.paidAmount,
            )
    }
}
