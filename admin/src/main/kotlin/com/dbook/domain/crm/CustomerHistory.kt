package com.dbook.domain.crm

import com.dbook.domain.common.PageQuery
import com.dbook.domain.common.PageResult
import java.math.BigDecimal
import java.time.LocalDateTime

// The flight fields are null for a booking of something that is not a flight (hotels, from M42 on).
data class CustomerBooking(
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
)

// Only the last four digits of the card: the support team never needs the cardholder's name.
data class CustomerPayment(
    val id: Long,
    val amount: BigDecimal,
    val cardLast4: String,
    val createdAt: LocalDateTime,
    val bookingIds: List<Long>,
)

data class CustomerReview(
    val id: Long,
    val bookingId: Long,
    val rating: Int,
    val comment: String?,
    val createdAt: LocalDateTime,
)

// Every list is newest first.
interface CustomerHistory {
    fun customerExists(id: Long): Boolean

    fun bookings(
        customerId: Long,
        page: PageQuery,
    ): PageResult<CustomerBooking>

    fun payments(
        customerId: Long,
        page: PageQuery,
    ): PageResult<CustomerPayment>

    fun reviews(
        customerId: Long,
        page: PageQuery,
    ): PageResult<CustomerReview>
}
