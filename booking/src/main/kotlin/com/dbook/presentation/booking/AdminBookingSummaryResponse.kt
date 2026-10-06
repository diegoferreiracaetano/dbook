package com.dbook.presentation.booking

import com.dbook.domain.booking.AdminBookingSummary
import com.dbook.domain.booking.BookingStatus
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime

/** `price` is the one frozen when the booking was made. The flight fields are null for what is not a flight. */
data class AdminBookingSummaryResponse(
    val id: Long,
    val status: BookingStatus,
    val price: BigDecimal,
    val discount: BigDecimal,
    val paidAmount: BigDecimal,
    val createdAt: Instant,
    val customerId: Long,
    val customerName: String,
    val bookableId: Long,
    val title: String,
    val seatLabel: String?,
    val checkIn: java.time.LocalDate?,
    val checkOut: java.time.LocalDate?,
    val flightNumber: String?,
    val origin: String?,
    val destination: String?,
    val departureTime: LocalDateTime?,
    val paymentId: Long?,
) {
    companion object {
        fun from(booking: AdminBookingSummary) =
            AdminBookingSummaryResponse(
                id = booking.id,
                status = booking.status,
                price = booking.price,
                discount = booking.discount,
                paidAmount = booking.price - booking.discount,
                createdAt = booking.createdAt,
                customerId = booking.customerId,
                customerName = booking.customerName,
                bookableId = booking.bookableId,
                title = booking.title,
                seatLabel = booking.seatLabel,
                checkIn = booking.checkIn,
                checkOut = booking.checkOut,
                flightNumber = booking.flightNumber,
                origin = booking.origin,
                destination = booking.destination,
                departureTime = booking.departureTime,
                paymentId = booking.paymentId,
            )
    }
}
