package com.dbook.domain.booking

import com.dbook.domain.common.PageQuery
import com.dbook.domain.common.PageResult
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

data class AdminBookingFilter(
    val status: BookingStatus? = null,
    val bookableId: Long? = null,
    val customerId: Long? = null,
    val createdFrom: Instant? = null,
    val createdTo: Instant? = null,
    val paid: Boolean? = null,
) {
    init {
        require(createdFrom == null || createdTo == null || !createdTo.isBefore(createdFrom)) {
            "createdTo must not be before createdFrom"
        }
    }
}

// The flight fields are null for a booking of something that is not a flight (hotels, from M42 on).
data class AdminBookingSummary(
    val id: Long,
    val status: BookingStatus,
    val price: BigDecimal,
    val discount: BigDecimal,
    val createdAt: Instant,
    val customerId: Long,
    val customerName: String,
    val bookableId: Long,
    val title: String,
    val seatLabel: String?,
    val flightNumber: String?,
    val origin: String?,
    val destination: String?,
    val departureTime: LocalDateTime?,
    val paymentId: Long?,
    val checkIn: LocalDate? = null,
    val checkOut: LocalDate? = null,
)

data class BookingTimelineEntry(
    val from: BookingStatus?,
    val to: BookingStatus,
    val actorId: Long?,
    val occurredAt: Instant,
)

data class BookingPaymentInfo(
    val id: Long,
    val amount: BigDecimal,
    val cardLast4: String,
    val createdAt: LocalDateTime,
)

data class BookingRefundInfo(
    val id: Long,
    val status: String,
    val amount: BigDecimal,
    val reason: String,
)

data class AdminBookingDetail(
    val booking: AdminBookingSummary,
    val payment: BookingPaymentInfo?,
    val refund: BookingRefundInfo?,
    val timeline: List<BookingTimelineEntry>,
)

interface AdminBookingReader {
    /** Newest first. */
    fun search(
        filter: AdminBookingFilter,
        page: PageQuery,
    ): PageResult<AdminBookingSummary>

    fun find(id: Long): AdminBookingDetail?
}
