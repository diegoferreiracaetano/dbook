package com.dbook.presentation.identity

import com.dbook.domain.identity.AppTheme
import com.dbook.domain.identity.CabinClassPreference
import com.dbook.domain.identity.DateFormatPreference
import com.dbook.domain.identity.DistanceUnit
import com.dbook.domain.identity.SeatPreference
import com.dbook.domain.identity.UserPreferences
import io.swagger.v3.oas.annotations.media.Schema

/** The whole set: a field left out (or null) clears that preference. */
data class PreferencesRequest(
    @get:Schema(example = "pt-BR", allowableValues = ["pt-BR", "en", "es"])
    val language: String? = null,
    val theme: AppTheme? = null,
    @get:Schema(example = "GRU", description = "IATA code of the airport the customer usually leaves from")
    val homeAirport: String? = null,
    @get:Schema(example = "BR", description = "ISO 3166 alpha-2")
    val country: String? = null,
    @get:Schema(example = "BRL", allowableValues = ["BRL", "USD", "EUR"])
    val currency: String? = null,
    val cabinClass: CabinClassPreference? = null,
    val seatPreference: SeatPreference? = null,
    val dateFormat: DateFormatPreference? = null,
    val distanceUnit: DistanceUnit? = null,
) {
    fun toPreferences() =
        UserPreferences(
            language,
            theme,
            homeAirport?.trim()?.uppercase(),
            country?.trim()?.uppercase(),
            currency?.trim()?.uppercase(),
            cabinClass,
            seatPreference,
            dateFormat,
            distanceUnit,
        )
}

data class PreferencesResponse(
    val language: String?,
    val theme: AppTheme?,
    val homeAirport: String?,
    val country: String?,
    val currency: String?,
    val cabinClass: CabinClassPreference?,
    val seatPreference: SeatPreference?,
    val dateFormat: DateFormatPreference?,
    val distanceUnit: DistanceUnit?,
) {
    companion object {
        fun from(preferences: UserPreferences) =
            PreferencesResponse(
                preferences.language, preferences.theme, preferences.homeAirport, preferences.country,
                preferences.currency, preferences.cabinClass, preferences.seatPreference, preferences.dateFormat,
                preferences.distanceUnit,
            )
    }
}

data class SessionResponse(
    @get:Schema(description = "Identifies the session; send it to DELETE /users/me/sessions/{id}")
    val id: String,
    @get:Schema(description = "When the session's token was last issued, i.e. the last time it was used")
    val lastActiveAt: java.time.Instant?,
    val expiresAt: java.time.Instant,
)

data class AvatarRequest(
    @get:Schema(example = "https://cdn.example.com/avatars/42.jpg", description = "https URL of the picture")
    val avatarUrl: String? = null,
)
