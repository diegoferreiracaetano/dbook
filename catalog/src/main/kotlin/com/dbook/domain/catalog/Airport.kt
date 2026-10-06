package com.dbook.domain.catalog

private val AIRPORT_CODE = Regex("^[A-Z]{3}$")

/** Reference data, resolved by IATA code (e.g. "GRU"): three capital letters. */
class Airport(
    val id: Long? = null,
    val iataCode: String,
    val name: String,
    val city: String,
    val country: String,
    val photoUrl: String,
    val region: String,
    val isPopular: Boolean,
) {
    init {
        require(AIRPORT_CODE.matches(iataCode)) { "an airport IATA code is 3 capital letters" }
        require(name.isNotBlank() && city.isNotBlank() && country.isNotBlank() && region.isNotBlank()) {
            "name, city, country and region must not be blank"
        }
    }
}
