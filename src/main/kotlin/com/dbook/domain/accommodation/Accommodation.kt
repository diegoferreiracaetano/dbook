package com.dbook.domain.accommodation

import com.dbook.domain.catalog.Airport
import com.dbook.domain.catalog.Bookable
import java.math.BigDecimal

private const val MAX_STARS = 5
private const val MAX_AMENITIES = 30

/**
 * A [Bookable] hotel. Its `price` is the lowest nightly rate of its room types ("from") and its capacity the number of
 * rooms; how many are free is a question of the night, answered by the [RoomInventory], so `availableCapacity` is not
 * a live number here (it equals the capacity).
 */
class Accommodation(
    id: Long? = null,
    val name: String,
    val destination: Airport,
    val address: String,
    val stars: Int,
    val description: String? = null,
    val photoUrl: String? = null,
    val amenities: Set<String> = emptySet(),
    val roomTypes: List<RoomType> = emptyList(),
    active: Boolean = true,
) : Bookable(
        id,
        name,
        roomTypes.filter { it.active }.minOfOrNull { it.nightlyRate } ?: BigDecimal.ZERO,
        roomTypes.sumOf { it.quantity },
        roomTypes.sumOf { it.quantity },
        active,
    ) {
    init {
        require(name.isNotBlank()) { "name must not be blank" }
        require(address.isNotBlank()) { "address must not be blank" }
        require(stars in 1..MAX_STARS) { "stars must be between 1 and $MAX_STARS" }
        require(amenities.size <= MAX_AMENITIES) { "at most $MAX_AMENITIES amenities" }
        // a name taken twice is a conflict (409), not a malformed request
        check(
            roomTypes.map { it.name.lowercase() }.toSet().size == roomTypes.size,
        ) { "room type names must be different" }
    }

    fun withRoomTypes(roomTypes: List<RoomType>) = copyWith(roomTypes = roomTypes)

    fun withActive(active: Boolean) = copyWith(active = active)

    fun withDetails(details: Details) =
        Accommodation(
            id, details.name, details.destination, details.address, details.stars, details.description,
            details.photoUrl, details.amenities, roomTypes, active,
        )

    /** What describes a hotel, apart from its room types. */
    data class Details(
        val name: String,
        val destination: Airport,
        val address: String,
        val stars: Int,
        val description: String?,
        val photoUrl: String?,
        val amenities: Set<String>,
    )

    private fun copyWith(
        roomTypes: List<RoomType> = this.roomTypes,
        active: Boolean = this.active,
    ) = Accommodation(id, name, destination, address, stars, description, photoUrl, amenities, roomTypes, active)
}
