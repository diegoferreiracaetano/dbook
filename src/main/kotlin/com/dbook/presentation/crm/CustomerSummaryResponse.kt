package com.dbook.presentation.crm

import com.dbook.domain.crm.CustomerSummary
import com.dbook.domain.identity.UserStatus
import java.time.Instant

data class CustomerSummaryResponse(
    val id: Long,
    val name: String,
    val email: String,
    val status: UserStatus,
    val createdAt: Instant,
    val lastLoginAt: Instant?,
    val bookingCount: Long,
) {
    companion object {
        fun from(summary: CustomerSummary) =
            CustomerSummaryResponse(
                id = summary.id,
                name = summary.name,
                email = summary.email,
                status = summary.status,
                createdAt = summary.createdAt,
                lastLoginAt = summary.lastLoginAt,
                bookingCount = summary.bookingCount,
            )
    }
}
