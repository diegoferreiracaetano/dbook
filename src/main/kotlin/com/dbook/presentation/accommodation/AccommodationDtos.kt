package com.dbook.presentation.accommodation

import com.dbook.application.accommodation.HotelDetails
import com.dbook.application.accommodation.RoomTypeData
import com.dbook.domain.accommodation.Accommodation
import com.dbook.domain.accommodation.AccommodationResult
import com.dbook.domain.accommodation.AvailableRoom
import com.dbook.domain.accommodation.RoomType
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.LocalDate

data class AvailableRoomResponse(
    val roomTypeId: Long,
    val name: String,
    val capacity: Int,
    val nightlyRate: BigDecimal,
    @get:Schema(description = "what the whole stay costs in this room: nights x nightlyRate")
    val totalPrice: BigDecimal,
) {
    companion object {
        fun from(room: AvailableRoom) =
            AvailableRoomResponse(room.roomTypeId, room.name, room.capacity, room.nightlyRate, room.totalPrice)
    }
}

data class AccommodationResultResponse(
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
    @get:Schema(description = "the cheapest stay among the rooms that are free")
    val fromPrice: BigDecimal,
    val rooms: List<AvailableRoomResponse>,
) {
    companion object {
        fun from(hotel: AccommodationResult) =
            AccommodationResultResponse(
                hotel.id, hotel.name, hotel.destinationIataCode, hotel.city, hotel.address, hotel.stars, hotel.photoUrl,
                hotel.amenities, hotel.averageRating, hotel.reviewCount, hotel.fromPrice,
                hotel.rooms.map(AvailableRoomResponse::from),
            )
    }
}

data class RoomTypeResponse(
    val id: Long?,
    val name: String,
    val capacity: Int,
    val nightlyRate: BigDecimal,
    val quantity: Int,
    val active: Boolean,
) {
    companion object {
        fun from(room: RoomType) =
            RoomTypeResponse(room.id, room.name, room.capacity, room.nightlyRate, room.quantity, room.active)
    }
}

data class AccommodationResponse(
    val id: Long?,
    val name: String,
    val destinationIataCode: String,
    val city: String,
    val address: String,
    val stars: Int,
    val description: String?,
    val photoUrl: String?,
    val amenities: Set<String>,
    val active: Boolean,
    val roomTypes: List<RoomTypeResponse>,
) {
    companion object {
        fun from(
            hotel: Accommodation,
            onlyActiveRooms: Boolean = false,
        ) = AccommodationResponse(
            hotel.id, hotel.name, hotel.destination.iataCode, hotel.destination.city, hotel.address, hotel.stars,
            hotel.description, hotel.photoUrl, hotel.amenities, hotel.active,
            hotel.roomTypes.filter { !onlyActiveRooms || it.active }.map(RoomTypeResponse::from),
        )
    }
}

data class StayBookingRequest(
    @get:Schema(example = "1", description = "the room type to book")
    val roomTypeId: Long,
    @get:Schema(example = "2027-01-15", description = "the first night")
    val checkIn: LocalDate,
    @get:Schema(example = "2027-01-18", description = "the day the guests leave: not a night")
    val checkOut: LocalDate,
    @get:Schema(example = "2")
    val guests: Int,
)

data class RoomTypeRequest(
    @get:Schema(example = "Double room")
    val name: String,
    @get:Schema(example = "2", description = "how many guests it takes")
    val capacity: Int,
    @get:Schema(example = "350.00")
    val nightlyRate: BigDecimal,
    @get:Schema(example = "10", description = "how many of this type the hotel has")
    val quantity: Int,
    // optional, on sale by default: a missing primitive would otherwise read as false (no Kotlin module here)
    val active: Boolean? = null,
) {
    fun toData() = RoomTypeData(name, capacity, nightlyRate, quantity, active ?: true)
}

data class AccommodationRequest(
    @get:Schema(example = "Hotel Copacabana")
    val name: String,
    @get:Schema(example = "GIG", description = "the destination: an airport that serves the hotel")
    val destinationIataCode: String,
    @get:Schema(example = "Av. Atlântica, 1000")
    val address: String,
    @get:Schema(example = "4")
    val stars: Int,
    val description: String? = null,
    val photoUrl: String? = null,
    @get:Schema(example = "[\"wifi\", \"pool\"]")
    val amenities: Set<String>? = null,
) {
    fun toDetails() =
        HotelDetails(name, destinationIataCode, address, stars, description, photoUrl, amenities.orEmpty())
}

data class CreateAccommodationRequest(
    val accommodation: AccommodationRequest,
    val roomTypes: List<RoomTypeRequest>? = null,
)
