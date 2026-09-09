package com.dbook.infrastructure.persistence

import com.dbook.domain.AiSuggestionLog
import com.dbook.domain.Airport
import com.dbook.domain.Bookable
import com.dbook.domain.Booking
import com.dbook.domain.Flight
import com.dbook.domain.RefreshToken
import com.dbook.domain.User

fun AirportJpaEntity.toDomain(): Airport =
    Airport(
        id = id,
        iataCode = iataCode,
        name = name,
        city = city,
        country = country,
    )

fun FlightJpaEntity.toDomain(): Flight =
    Flight(
        id = id,
        title = title,
        price = price,
        totalCapacity = totalCapacity,
        availableCapacity = availableCapacity,
        active = active,
        flightNumber = flightNumber,
        origin = origin.toDomain(),
        destination = destination.toDomain(),
        departureTime = departureTime,
        arrivalTime = arrivalTime,
        seatClass = seatClass,
    )

// Bookable is abstract: whatever arrives here at runtime is always a concrete
// specialization (today only FlightJpaEntity; Accommodation joins in M9 with a new `is`).
fun BookableJpaEntity.toDomain(): Bookable =
    when (this) {
        is FlightJpaEntity -> this.toDomain()
        else -> error("Unknown Bookable subtype: ${this::class}")
    }

fun BookingJpaEntity.toDomain(): Booking =
    Booking(
        id = id,
        bookable = bookable.toDomain(),
        customerId = customerId,
        status = status,
    )

fun Flight.toJpaEntity(
    origin: AirportJpaEntity,
    destination: AirportJpaEntity,
): FlightJpaEntity =
    FlightJpaEntity(
        id = id,
        title = title,
        price = price,
        totalCapacity = totalCapacity,
        availableCapacity = availableCapacity,
        active = active,
        flightNumber = flightNumber,
        origin = origin,
        destination = destination,
        departureTime = departureTime,
        arrivalTime = arrivalTime,
        seatClass = seatClass,
    )

fun Booking.toJpaEntity(bookable: BookableJpaEntity): BookingJpaEntity =
    BookingJpaEntity(
        id = id,
        bookable = bookable,
        customerId = customerId,
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
