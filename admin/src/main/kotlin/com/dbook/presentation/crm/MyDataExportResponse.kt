package com.dbook.presentation.crm

import com.dbook.application.crm.MyDataExport
import com.dbook.domain.favorite.FavoriteType
import java.time.Instant

data class MyDataExportResponse(
    val profile: Profile,
    val bookings: List<CustomerBookingResponse>,
    val payments: List<CustomerPaymentResponse>,
    val reviews: List<CustomerReviewResponse>,
    val favorites: List<FavoriteEntry>,
) {
    data class FavoriteEntry(val type: FavoriteType, val id: String, val createdAt: Instant)

    data class Profile(val id: Long?, val name: String, val email: String, val lastLoginAt: Instant?)

    companion object {
        fun from(export: MyDataExport) =
            MyDataExportResponse(
                profile = Profile(export.user.id, export.user.name, export.user.email, export.user.lastLoginAt),
                bookings = export.bookings.map(CustomerBookingResponse::from),
                payments = export.payments.map(CustomerPaymentResponse::from),
                reviews = export.reviews.map(CustomerReviewResponse::from),
                favorites = export.favorites.map { FavoriteEntry(it.type, it.targetId, it.createdAt) },
            )
    }
}
