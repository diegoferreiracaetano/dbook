package com.dbook.presentation.crm

import com.dbook.application.crm.MyDataExport
import com.dbook.domain.favorite.FavoriteType
import java.time.Instant

data class MyDataExportResponse(
    val profile: Profile,
    val preferences: Preferences,
    val bookings: List<CustomerBookingResponse>,
    val payments: List<CustomerPaymentResponse>,
    val reviews: List<CustomerReviewResponse>,
    val favorites: List<FavoriteEntry>,
) {
    data class FavoriteEntry(val type: FavoriteType, val id: String, val createdAt: Instant)

    data class Profile(
        val id: Long?,
        val name: String,
        val email: String,
        val lastLoginAt: Instant?,
        val createdAt: Instant?,
        val avatarUrl: String?,
    )

    data class Preferences(
        val language: String?,
        val theme: String?,
        val homeAirport: String?,
        val country: String?,
        val currency: String?,
        val cabinClass: String?,
        val seatPreference: String?,
        val dateFormat: String?,
        val distanceUnit: String?,
    )

    companion object {
        fun from(export: MyDataExport) =
            MyDataExportResponse(
                profile =
                    Profile(
                        export.user.id,
                        export.user.name,
                        export.user.email,
                        export.user.lastLoginAt,
                        export.user.createdAt,
                        export.profile.avatarUrl,
                    ),
                preferences =
                    export.profile.preferences.let {
                        Preferences(
                            it.language,
                            it.theme?.name,
                            it.homeAirport,
                            it.country,
                            it.currency,
                            it.cabinClass?.name,
                            it.seatPreference?.name,
                            it.dateFormat?.name,
                            it.distanceUnit?.name,
                        )
                    },
                bookings = export.bookings.map(CustomerBookingResponse::from),
                payments = export.payments.map(CustomerPaymentResponse::from),
                reviews = export.reviews.map(CustomerReviewResponse::from),
                favorites = export.favorites.map { FavoriteEntry(it.type, it.targetId, it.createdAt) },
            )
    }
}
