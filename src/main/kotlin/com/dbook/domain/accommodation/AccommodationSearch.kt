package com.dbook.domain.accommodation

import com.dbook.domain.common.PageQuery
import com.dbook.domain.common.PageResult
import java.math.BigDecimal
import java.time.LocalDate

/** What the customer asked for: hotels at a destination, free for every night of the stay, for this many guests. */
data class AccommodationSearch(
    val destinationIataCode: String,
    val checkIn: LocalDate,
    val checkOut: LocalDate,
    val guests: Int,
) {
    init {
        require(destinationIataCode.length == IATA_LENGTH && destinationIataCode.all { it in 'A'..'Z' }) {
            "destination is a 3-letter IATA code in capitals"
        }
        require(checkOut.isAfter(checkIn)) { "checkOut must be after checkIn" }
        require(guests >= 1) { "guests must be at least 1" }
    }

    val nights: Long get() = java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut)

    private companion object {
        const val IATA_LENGTH = 3
    }
}

/** A room type that is free for the whole stay, with what the stay costs in it. */
data class AvailableRoom(
    val roomTypeId: Long,
    val name: String,
    val capacity: Int,
    val nightlyRate: BigDecimal,
    val totalPrice: BigDecimal,
)

data class AccommodationResult(
    val id: Long,
    val name: String,
    val destinationIataCode: String,
    val city: String,
    val address: String,
    val stars: Int,
    val photoUrl: String?,
    val amenities: Set<String>,
    val averageRating: Double?,
    val reviewCount: Long,
    val fromPrice: BigDecimal,
    val rooms: List<AvailableRoom>,
)

interface AccommodationSearcher {
    /** Hotels with at least one room type free for every night, cheapest stay first. Only active ones. */
    fun search(
        search: AccommodationSearch,
        page: PageQuery,
    ): PageResult<AccommodationResult>
}
