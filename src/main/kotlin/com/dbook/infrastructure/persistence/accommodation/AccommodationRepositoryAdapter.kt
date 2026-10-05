package com.dbook.infrastructure.persistence.accommodation

import com.dbook.domain.accommodation.Accommodation
import com.dbook.domain.accommodation.AccommodationNotFoundException
import com.dbook.domain.accommodation.AccommodationRepository
import com.dbook.domain.accommodation.RoomType
import com.dbook.infrastructure.persistence.catalog.AccommodationJpaEntity
import com.dbook.infrastructure.persistence.catalog.AccommodationJpaRepository
import com.dbook.infrastructure.persistence.catalog.AirportJpaRepository
import com.dbook.infrastructure.persistence.catalog.toDomain
import org.springframework.dao.DuplicateKeyException
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Repository
class AccommodationRepositoryAdapter(
    private val jpa: AccommodationJpaRepository,
    private val airports: AirportJpaRepository,
    private val jdbc: NamedParameterJdbcTemplate,
) : AccommodationRepository {
    @Transactional(readOnly = true)
    override fun findById(id: Long): Accommodation? = jpa.findById(id).orElse(null)?.toDomain(roomTypesOf(id))

    @Transactional
    override fun save(accommodation: Accommodation): Accommodation {
        val destination = airports.getReferenceById(requireNotNull(accommodation.destination.id))
        val entity =
            accommodation.id?.let { jpa.findById(it).orElseThrow { AccommodationNotFoundException(it) } }
                ?: AccommodationJpaEntity(destination = destination)
        entity.title = accommodation.name
        entity.destination = destination
        entity.address = accommodation.address
        entity.stars = accommodation.stars
        entity.description = accommodation.description
        entity.photoUrl = accommodation.photoUrl
        entity.amenities = accommodation.amenities.sorted().joinToString(",")
        entity.price = accommodation.price
        entity.totalCapacity = accommodation.totalCapacity
        entity.active = accommodation.active
        val saved = jpa.saveAndFlush(entity)
        val id = requireNotNull(saved.id)
        try {
            accommodation.roomTypes.forEach { saveRoomType(id, it) }
        } catch (ex: DuplicateKeyException) {
            throw IllegalStateException("A room type with this name already exists in this hotel", ex)
        }
        return requireNotNull(findById(id))
    }

    @Transactional(readOnly = true)
    override fun findRoomType(roomTypeId: Long): Pair<Accommodation, RoomType>? {
        val owner =
            jdbc.query(
                "SELECT accommodation_id FROM room_type WHERE id = :id",
                mapOf("id" to roomTypeId),
            ) { rs, _ -> rs.getLong("accommodation_id") }.firstOrNull()
        val hotel = owner?.let { findById(it) }
        return hotel?.roomTypes?.find { it.id == roomTypeId }?.let { hotel to it }
    }

    private fun saveRoomType(
        accommodationId: Long,
        roomType: RoomType,
    ) {
        val params =
            mapOf(
                "accommodation" to accommodationId,
                "name" to roomType.name,
                "capacity" to roomType.capacity,
                "rate" to roomType.nightlyRate,
                "quantity" to roomType.quantity,
                "active" to roomType.active,
            )
        if (roomType.id == null) {
            jdbc.update(
                "INSERT INTO room_type (accommodation_id, name, capacity, nightly_rate, quantity, active) " +
                    "VALUES (:accommodation, :name, :capacity, :rate, :quantity, :active)",
                params,
            )
        } else {
            jdbc.update(
                "UPDATE room_type SET name = :name, capacity = :capacity, nightly_rate = :rate, " +
                    "quantity = :quantity, active = :active WHERE id = :id AND accommodation_id = :accommodation",
                params + ("id" to roomType.id),
            )
        }
    }

    private fun roomTypesOf(accommodationId: Long): List<RoomType> =
        jdbc.query(
            "SELECT id, name, capacity, nightly_rate, quantity, active FROM room_type " +
                "WHERE accommodation_id = :id ORDER BY id",
            mapOf("id" to accommodationId),
        ) { rs, _ ->
            RoomType(
                id = rs.getLong("id"),
                name = rs.getString("name"),
                capacity = rs.getInt("capacity"),
                nightlyRate = rs.getBigDecimal("nightly_rate"),
                quantity = rs.getInt("quantity"),
                active = rs.getBoolean("active"),
            )
        }
}

internal fun zeroIfNull(value: BigDecimal?): BigDecimal = value ?: BigDecimal.ZERO
