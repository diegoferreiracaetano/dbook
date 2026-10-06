package com.dbook.presentation.crm

import com.dbook.domain.crm.CustomerProfile
import com.dbook.domain.identity.UserStatus
import java.math.BigDecimal
import java.time.Instant

data class CustomerProfileResponse(
    val id: Long,
    val name: String,
    val email: String,
    val status: UserStatus,
    val blockedReason: String?,
    val blockedAt: Instant?,
    val createdAt: Instant,
    val lastLoginAt: Instant?,
    val anonymizedAt: Instant?,
    val bookings: BookingTotalsResponse,
    val payments: PaymentTotalsResponse,
    val reviews: ReviewTotalsResponse,
) {
    data class BookingTotalsResponse(val total: Long, val pending: Long, val confirmed: Long, val cancelled: Long)

    data class PaymentTotalsResponse(
        val count: Long,
        val totalPaid: BigDecimal,
    )

    data class ReviewTotalsResponse(val count: Long, val averageRating: Double?)

    companion object {
        fun from(profile: CustomerProfile) =
            CustomerProfileResponse(
                id = profile.id,
                name = profile.name,
                email = profile.email,
                status = profile.status,
                blockedReason = profile.blockedReason,
                blockedAt = profile.blockedAt,
                createdAt = profile.createdAt,
                lastLoginAt = profile.lastLoginAt,
                anonymizedAt = profile.anonymizedAt,
                bookings =
                    BookingTotalsResponse(
                        total = profile.bookings.total,
                        pending = profile.bookings.pending,
                        confirmed = profile.bookings.confirmed,
                        cancelled = profile.bookings.cancelled,
                    ),
                payments = PaymentTotalsResponse(profile.payments.count, profile.payments.totalPaid),
                reviews = ReviewTotalsResponse(profile.reviews.count, profile.reviews.averageRating),
            )
    }
}
