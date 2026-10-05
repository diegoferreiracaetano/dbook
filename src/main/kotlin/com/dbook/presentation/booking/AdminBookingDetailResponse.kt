package com.dbook.presentation.booking

import com.dbook.domain.booking.AdminBookingDetail
import com.dbook.domain.booking.BookingStatus
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime

data class AdminBookingDetailResponse(
    val booking: AdminBookingSummaryResponse,
    val payment: PaymentInfo?,
    val refund: RefundInfo?,
    val timeline: List<TimelineEntry>,
) {
    data class PaymentInfo(val id: Long, val amount: BigDecimal, val cardLast4: String, val createdAt: LocalDateTime)

    data class RefundInfo(val id: Long, val status: String, val amount: BigDecimal, val reason: String)

    /** `actorId` is null when the system did it (the expiry job). `from` is null on the creation entry. */
    data class TimelineEntry(
        val from: BookingStatus?,
        val to: BookingStatus,
        val actorId: Long?,
        val occurredAt: Instant,
    )

    companion object {
        fun from(detail: AdminBookingDetail) =
            AdminBookingDetailResponse(
                booking = AdminBookingSummaryResponse.from(detail.booking),
                payment = detail.payment?.let { PaymentInfo(it.id, it.amount, it.cardLast4, it.createdAt) },
                refund = detail.refund?.let { RefundInfo(it.id, it.status, it.amount, it.reason) },
                timeline = detail.timeline.map { TimelineEntry(it.from, it.to, it.actorId, it.occurredAt) },
            )
    }
}
