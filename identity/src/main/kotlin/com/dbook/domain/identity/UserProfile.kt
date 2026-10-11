package com.dbook.domain.identity

private const val URL_MAX_LENGTH = 500
private val LANGUAGES = setOf("pt-BR", "en", "es")
private val CURRENCIES = setOf("BRL", "USD", "EUR")
private val AIRPORT_CODE = Regex("^[A-Z]{3}$")
private val COUNTRY_CODE = Regex("^[A-Z]{2}$")

enum class AppTheme { SYSTEM, LIGHT, DARK }

enum class CabinClassPreference { ECONOMY, PREMIUM_ECONOMY, BUSINESS, FIRST }

enum class SeatPreference { ANY, WINDOW, AISLE }

enum class DateFormatPreference { DMY, MDY, YMD }

enum class DistanceUnit { KM, MI }

/**
 * What a customer prefers. Every field is optional: null means "no choice", and the client falls back to its own
 * default. Stored as given even when no screen uses it yet, so a preference never has to be asked for twice.
 */
data class UserPreferences(
    val language: String? = null,
    val theme: AppTheme? = null,
    val homeAirport: String? = null,
    val country: String? = null,
    val currency: String? = null,
    val cabinClass: CabinClassPreference? = null,
    val seatPreference: SeatPreference? = null,
    val dateFormat: DateFormatPreference? = null,
    val distanceUnit: DistanceUnit? = null,
) {
    init {
        require(language == null || language in LANGUAGES) { "language must be one of $LANGUAGES" }
        require(homeAirport == null || AIRPORT_CODE.matches(homeAirport)) {
            "homeAirport must be an IATA code of 3 capital letters"
        }
        require(country == null || COUNTRY_CODE.matches(country)) {
            "country must be an ISO 3166 code of 2 capital letters"
        }
        require(currency == null || currency in CURRENCIES) { "currency must be one of $CURRENCIES" }
    }
}

/** The picture and the preferences of one user; absent from the database until the first change. */
data class UserProfile(
    val userId: Long,
    val avatarUrl: String? = null,
    val preferences: UserPreferences = UserPreferences(),
) {
    init {
        require(avatarUrl == null || (avatarUrl.startsWith("https://") && avatarUrl.length <= URL_MAX_LENGTH)) {
            "the picture must be an https URL of up to $URL_MAX_LENGTH characters"
        }
    }

    fun withAvatar(url: String?) = copy(avatarUrl = url)

    fun withPreferences(preferences: UserPreferences) = copy(preferences = preferences)
}

/** Persistence port for [UserProfile]. */
interface UserProfileRepository {
    fun find(userId: Long): UserProfile?

    fun save(profile: UserProfile): UserProfile
}
