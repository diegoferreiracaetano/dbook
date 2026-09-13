package com.dbook.infrastructure.persistence

import com.dbook.domain.AiSuggestionLog
import com.dbook.domain.Airline
import com.dbook.domain.Airport
import com.dbook.domain.Bookable
import com.dbook.domain.Booking
import com.dbook.domain.Flight
import com.dbook.domain.RefreshToken
import com.dbook.domain.Seat
import com.dbook.domain.User

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
    )

// Bookable is abstract: whatever arrives here at runtime is always a concrete
// specialization (today only FlightJpaEntity; Accommodation joins in M9 with a new `is`).
fun BookableJpaEntity.toDomain(availableCapacity: Int): Bookable =
    when (this) {
        is FlightJpaEntity -> this.toDomain(availableCapacity)
        else -> error("Unknown Bookable subtype: ${this::class}")
    }

fun BookingJpaEntity.toDomain(availableCapacity: Int): Booking =
    Booking(
        id = id,
        bookable = bookable.toDomain(availableCapacity),
        seatId = seat.id ?: error("A persisted Booking must reference a persisted Seat"),
        customerId = customerId,
        status = status,
    )

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
    )

fun Booking.toJpaEntity(
    bookable: BookableJpaEntity,
    seat: SeatJpaEntity,
): BookingJpaEntity =
    BookingJpaEntity(
        id = id,
        bookable = bookable,
        seat = seat,
        customerId = customerId,
        status = status,
    )

fun SeatJpaEntity.toDomain(): Seat =
    Seat(
        id = id,
        bookableId = bookable.id ?: error("A persisted Seat must reference a persisted Bookable"),
        label = label,
        status = status,
    )

fun Seat.toJpaEntity(bookable: BookableJpaEntity): SeatJpaEntity =
    SeatJpaEntity(
        id = id,
        bookable = bookable,
        label = label,
        status = status,
    )

fun UserJpaEntity.toDomain(): User =
    User(
        id = id,
        email = email,
        passwordHash = passwordHash,
        role = role,
    )

fun User.toJpaEntity(): UserJpaEntity =
    UserJpaEntity(
        id = id,
        email = email,
        passwordHash = passwordHash,
        role = role,
    )

fun RefreshTokenJpaEntity.toDomain(): RefreshToken =
    RefreshToken(
        id = id,
        userId = userId,
        tokenHash = tokenHash,
        expiresAt = expiresAt,
        revoked = revoked,
    )

fun RefreshToken.toJpaEntity(): RefreshTokenJpaEntity =
    RefreshTokenJpaEntity(
        id = id,
        userId = userId,
        tokenHash = tokenHash,
        expiresAt = expiresAt,
        revoked = revoked,
    )

fun AiSuggestionLogJpaEntity.toDomain(): AiSuggestionLog =
    AiSuggestionLog(
        id = id,
        userId = userId,
        query = query,
        rawResponse = rawResponse,
        createdAt = createdAt,
    )

fun AiSuggestionLog.toJpaEntity(): AiSuggestionLogJpaEntity =
    AiSuggestionLogJpaEntity(
        id = id,
        userId = userId,
        query = query,
        rawResponse = rawResponse,
        createdAt = createdAt,
    )
