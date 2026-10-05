package com.dbook.infrastructure.persistence.catalog

import com.dbook.domain.accommodation.Accommodation
import com.dbook.domain.accommodation.RoomType
import com.dbook.domain.catalog.Airline
import com.dbook.domain.catalog.Airport
import com.dbook.domain.catalog.Bookable
import com.dbook.domain.catalog.Flight

fun AirportJpaEntity.toDomain(): Airport =
    Airport(
        id = id,
        iataCode = iataCode,
        name = name,
        city = city,
        country = country,
        photoUrl = photoUrl,
        region = region,
        isPopular = isPopular,
    )

fun AirlineJpaEntity.toDomain(): Airline =
    Airline(
        id = id,
        iataCode = iataCode,
        name = name,
    )

// availableCapacity is no longer stored on the entity (V9) — derived from AVAILABLE seats,
// so callers must supply the count they already queried instead of it being read off `this`.
fun FlightJpaEntity.toDomain(availableCapacity: Int): Flight =
    Flight(
        id = id,
        title = title,
        price = price,
        totalCapacity = totalCapacity,
        availableCapacity = availableCapacity,
        active = active,
        flightNumber = flightNumber,
        airline = airline.toDomain(),
        origin = origin.toDomain(),
        destination = destination.toDomain(),
        departureTime = departureTime,
        arrivalTime = arrivalTime,
        seatClass = seatClass,
        aircraftType = aircraftType,
    )

// Bookable is abstract: whatever arrives here at runtime is always a concrete
// specialization (today only FlightJpaEntity; Accommodation joins in M9 with a new `is`).
fun BookableJpaEntity.toDomain(availableCapacity: Int): Bookable =
    when (this) {
        is FlightJpaEntity -> this.toDomain(availableCapacity)
        // the light form (no room types): a booking only needs to know which hotel it is at
        is AccommodationJpaEntity -> this.toDomain(emptyList())
        else -> error("Unknown Bookable subtype: ${this::class}")
    }

fun Flight.toJpaEntity(
    airline: AirlineJpaEntity,
    origin: AirportJpaEntity,
    destination: AirportJpaEntity,
): FlightJpaEntity =
    FlightJpaEntity(
        id = id,
        title = title,
        price = price,
        totalCapacity = totalCapacity,
        active = active,
        flightNumber = flightNumber,
        airline = airline,
        origin = origin,
        destination = destination,
        departureTime = departureTime,
        arrivalTime = arrivalTime,
        seatClass = seatClass,
        aircraftType = aircraftType,
    )

fun AccommodationJpaEntity.toDomain(roomTypes: List<RoomType>): Accommodation =
    Accommodation(
        id = id,
        name = title,
        destination = destination.toDomain(),
        address = address,
        stars = stars,
        description = description,
        photoUrl = photoUrl,
        amenities = amenities.split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet(),
        roomTypes = roomTypes,
        active = active,
    )
