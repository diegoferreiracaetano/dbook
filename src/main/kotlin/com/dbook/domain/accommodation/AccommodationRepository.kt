package com.dbook.domain.accommodation

interface AccommodationRepository {
    fun findById(id: Long): Accommodation?

    /** Creates (no id) or changes (with id) the hotel and its room types; a room type with no id is a new one. */
    fun save(accommodation: Accommodation): Accommodation

    /** The room type and the hotel it belongs to, or null. */
    fun findRoomType(roomTypeId: Long): Pair<Accommodation, RoomType>?
}

class AccommodationNotFoundException(id: Long) : RuntimeException("Accommodation not found: $id")

class RoomTypeNotFoundException(id: Long) : RuntimeException("Room type not found: $id")
