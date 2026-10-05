package com.dbook.application.accommodation

import com.dbook.domain.accommodation.Accommodation
import com.dbook.domain.accommodation.AccommodationRepository
import com.dbook.domain.accommodation.RoomType

class InMemoryAccommodations(vararg initial: Accommodation) : AccommodationRepository {
    private val hotels = initial.toMutableList()
    private var lastRoomId = 100L

    override fun findById(id: Long): Accommodation? = hotels.find { it.id == id }

    override fun save(accommodation: Accommodation): Accommodation {
        val rooms = accommodation.roomTypes.map { if (it.id == null) it.copy(id = ++lastRoomId) else it }
        val saved =
            if (accommodation.id == null) {
                accommodation.withRoomTypes(rooms).let { rebuilt ->
                    Accommodation(
                        hotels.size + 1L, rebuilt.name, rebuilt.destination, rebuilt.address, rebuilt.stars,
                        rebuilt.description, rebuilt.photoUrl, rebuilt.amenities, rooms, rebuilt.active,
                    )
                }
            } else {
                accommodation.withRoomTypes(rooms)
            }
        hotels.removeAll { it.id == saved.id }
        hotels += saved
        return saved
    }

    override fun findRoomType(roomTypeId: Long): Pair<Accommodation, RoomType>? =
        hotels.firstNotNullOfOrNull { hotel -> hotel.roomTypes.find { it.id == roomTypeId }?.let { hotel to it } }
}
