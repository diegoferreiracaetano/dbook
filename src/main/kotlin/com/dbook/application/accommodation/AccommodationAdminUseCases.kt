package com.dbook.application.accommodation

import com.dbook.application.catalog.AirportLookup
import com.dbook.domain.accommodation.Accommodation
import com.dbook.domain.accommodation.AccommodationNotFoundException
import com.dbook.domain.accommodation.AccommodationRepository
import com.dbook.domain.accommodation.AdminAccommodationReader
import com.dbook.domain.accommodation.RoomInventory
import com.dbook.domain.accommodation.RoomType
import com.dbook.domain.accommodation.RoomTypeNotFoundException
import com.dbook.domain.common.PageQuery
import com.dbook.domain.common.PageResult
import com.dbook.domain.common.access.Actor
import com.dbook.domain.common.audit.AuditAction
import com.dbook.domain.common.audit.AuditEvent
import com.dbook.domain.common.audit.AuditLog
import io.micrometer.observation.annotation.Observed
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate

data class HotelDetails(
    val name: String,
    val destinationIataCode: String,
    val address: String,
    val stars: Int,
    val description: String?,
    val photoUrl: String?,
    val amenities: Set<String>,
)

data class RoomTypeData(
    val name: String,
    val capacity: Int,
    val nightlyRate: BigDecimal,
    val quantity: Int,
    val active: Boolean = true,
)

private fun RoomTypeData.toRoomType(id: Long? = null) =
    RoomType(id, name.trim(), capacity, nightlyRate, quantity, active)

/** What the audit trail keeps of a hotel: the facts, not the free text. */
fun Accommodation.toAuditSnapshot(): Map<String, Any?> =
    mapOf(
        "id" to id,
        "name" to name,
        "destination" to destination.iataCode,
        "stars" to stars,
        "active" to active,
        "roomTypes" to roomTypes.map { "${it.name}:${it.quantity}x${it.nightlyRate.toPlainString()}:${it.active}" },
    )

private fun Accommodation.audit(
    actor: Actor,
    action: AuditAction,
    before: Accommodation?,
) = AuditEvent(
    actor,
    action,
    requireNotNull(id).toString(),
    before = before?.toAuditSnapshot(),
    after = toAuditSnapshot(),
)

/** Creates a hotel with its first room types. Audited. */
@Observed(name = "dbook.usecase")
@Service
class CreateAccommodationUseCase(
    private val accommodations: AccommodationRepository,
    private val airportLookup: AirportLookup,
    private val auditLog: AuditLog,
) {
    @Transactional
    fun execute(
        actor: Actor,
        details: HotelDetails,
        rooms: List<RoomTypeData>,
    ): Accommodation {
        val saved =
            accommodations.save(
                Accommodation(
                    name = details.name.trim(),
                    destination = airportLookup.airport(details.destinationIataCode),
                    address = details.address.trim(),
                    stars = details.stars,
                    description = details.description?.trim()?.takeIf { it.isNotEmpty() },
                    photoUrl = details.photoUrl?.trim()?.takeIf { it.isNotEmpty() },
                    amenities = details.amenities.map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet(),
                    roomTypes =
                        rooms.map {
                            RoomType(
                                name = it.name.trim(),
                                capacity = it.capacity,
                                nightlyRate = it.nightlyRate,
                                quantity = it.quantity,
                                active = it.active,
                            )
                        },
                ),
            )
        auditLog.record(saved.audit(actor, AuditAction.ACCOMMODATION_CREATED, before = null))
        return saved
    }
}

/** Changes what describes the hotel (not its room types). Audited. */
@Observed(name = "dbook.usecase")
@Service
class UpdateAccommodationUseCase(
    private val accommodations: AccommodationRepository,
    private val airportLookup: AirportLookup,
    private val auditLog: AuditLog,
) {
    @Transactional
    fun execute(
        actor: Actor,
        id: Long,
        details: HotelDetails,
    ): Accommodation {
        val before = accommodations.findById(id) ?: throw AccommodationNotFoundException(id)
        val saved =
            accommodations.save(
                before.withDetails(
                    Accommodation.Details(
                        details.name.trim(),
                        airportLookup.airport(details.destinationIataCode),
                        details.address.trim(),
                        details.stars,
                        details.description?.trim()?.takeIf { it.isNotEmpty() },
                        details.photoUrl?.trim()?.takeIf { it.isNotEmpty() },
                        details.amenities.map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet(),
                    ),
                ),
            )
        auditLog.record(saved.audit(actor, AuditAction.ACCOMMODATION_UPDATED, before))
        return saved
    }
}

/** Takes a hotel off sale (it stops showing and stops taking bookings; the bookings made stay) or puts it back. */
@Observed(name = "dbook.usecase")
@Service
class SetAccommodationActiveUseCase(
    private val accommodations: AccommodationRepository,
    private val auditLog: AuditLog,
) {
    @Transactional
    fun execute(
        actor: Actor,
        id: Long,
        active: Boolean,
    ): Accommodation {
        val before = accommodations.findById(id) ?: throw AccommodationNotFoundException(id)
        check(before.active != active) { "${before.name} is already ${if (active) "on" else "off"} sale" }
        val saved = accommodations.save(before.withActive(active))
        auditLog.record(
            saved.audit(
                actor,
                if (active) AuditAction.ACCOMMODATION_ACTIVATED else AuditAction.ACCOMMODATION_DEACTIVATED,
                before,
            ),
        )
        return saved
    }
}

/** Adds a room type to a hotel. Audited. */
@Observed(name = "dbook.usecase")
@Service
class AddRoomTypeUseCase(
    private val accommodations: AccommodationRepository,
    private val auditLog: AuditLog,
) {
    @Transactional
    fun execute(
        actor: Actor,
        accommodationId: Long,
        room: RoomTypeData,
    ): Accommodation {
        val before = accommodations.findById(accommodationId) ?: throw AccommodationNotFoundException(accommodationId)
        val saved =
            accommodations.save(
                before.withRoomTypes(
                    before.roomTypes +
                        room.toRoomType(),
                ),
            )
        auditLog.record(saved.audit(actor, AuditAction.ROOM_TYPE_CHANGED, before))
        return saved
    }
}

/**
 * Changes a room type: its rate (bookings made keep the rate they froze), its capacity, its name, whether it is on
 * sale, and how many there are: **never fewer than the most already booked on any night from today on**, or a booking
 * would be left without a room. Audited.
 */
@Observed(name = "dbook.usecase")
@Service
class UpdateRoomTypeUseCase(
    private val accommodations: AccommodationRepository,
    private val roomInventory: RoomInventory,
    private val auditLog: AuditLog,
    private val clock: Clock,
) {
    @Transactional
    fun execute(
        actor: Actor,
        accommodationId: Long,
        roomTypeId: Long,
        room: RoomTypeData,
    ): Accommodation {
        val before = accommodations.findById(accommodationId) ?: throw AccommodationNotFoundException(accommodationId)
        val current = before.roomTypes.find { it.id == roomTypeId } ?: throw RoomTypeNotFoundException(roomTypeId)
        val booked = roomInventory.maxBookedFrom(roomTypeId, LocalDate.now(clock))
        check(room.quantity >= booked) {
            "$booked rooms of ${current.name} are already booked on some night: quantity cannot be below that"
        }
        val changed =
            RoomType(roomTypeId, room.name.trim(), room.capacity, room.nightlyRate, room.quantity, room.active)
        val saved =
            accommodations.save(
                before.withRoomTypes(before.roomTypes.map { if (it.id == roomTypeId) changed else it }),
            )
        auditLog.record(saved.audit(actor, AuditAction.ROOM_TYPE_CHANGED, before))
        return saved
    }
}

@Observed(name = "dbook.usecase")
@Service
class ListAccommodationsUseCase(
    private val reader: AdminAccommodationReader,
    private val accommodations: AccommodationRepository,
) {
    fun search(
        destinationIataCode: String?,
        page: PageQuery,
    ): PageResult<Accommodation> = reader.search(destinationIataCode, page)

    fun find(id: Long): Accommodation = accommodations.findById(id) ?: throw AccommodationNotFoundException(id)
}
