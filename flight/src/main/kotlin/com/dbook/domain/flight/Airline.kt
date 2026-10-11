package com.dbook.domain.flight

private val AIRLINE_CODE = Regex("^[A-Z0-9]{2}$")
private const val LOGO_MAX_LENGTH = 500

/** Reference data, resolved by IATA code (e.g. "LA"): two letters or digits. */
class Airline(
    val id: Long? = null,
    val iataCode: String,
    val name: String,
    val logoUrl: String? = null,
) {
    init {
        require(AIRLINE_CODE.matches(iataCode)) { "an airline IATA code is 2 capital letters or digits" }
        require(name.isNotBlank()) { "name must not be blank" }
        require(logoUrl == null || (logoUrl.startsWith("https://") && logoUrl.length <= LOGO_MAX_LENGTH)) {
            "the logo must be an https URL of up to $LOGO_MAX_LENGTH characters"
        }
    }
}
