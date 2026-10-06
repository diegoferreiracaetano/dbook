package com.dbook.domain.crm

import com.dbook.domain.identity.UserStatus
import java.math.BigDecimal
import java.time.Instant

data class BookingTotals(
    val pending: Long,
    val confirmed: Long,
    val cancelled: Long,
) {
    val total: Long get() = pending + confirmed + cancelled
}

data class PaymentTotals(
    val count: Long,
    val totalPaid: BigDecimal,
)

data class ReviewTotals(
    val count: Long,
    val averageRating: Double?,
)

data class CustomerProfile(
    val id: Long,
    val name: String,
    val email: String,
    val status: UserStatus,
    val blockedReason: String?,
    val blockedAt: Instant?,
    val createdAt: Instant,
    val lastLoginAt: Instant?,
    val anonymizedAt: Instant?,
    val bookings: BookingTotals,
    val payments: PaymentTotals,
    val reviews: ReviewTotals,
)

interface CustomerProfileReader {
    fun find(id: Long): CustomerProfile?
}
