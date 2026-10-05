package com.dbook.domain.catalog

private val AIRLINE_CODE = Regex("^[A-Z0-9]{2}$")

/** Reference data, resolved by IATA code (e.g. "LA"): two letters or digits. */
class Airline(
    val id: Long? = null,
    val iataCode: String,
    val name: String,
) {
    init {
        require(AIRLINE_CODE.matches(iataCode)) { "an airline IATA code is 2 capital letters or digits" }
        require(name.isNotBlank()) { "name must not be blank" }
    }
}
