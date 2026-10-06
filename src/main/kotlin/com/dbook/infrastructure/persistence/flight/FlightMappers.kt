// ktlint treats an import of `toDomain` as unused when the file declares one of its own: these overloads live in one
// package per concept, so the imports are needed even though they look redundant to it.
@file:Suppress("ktlint:standard:no-unused-imports")

package com.dbook.infrastructure.persistence.flight

import com.dbook.domain.flight.Airline
import com.dbook.domain.flight.Flight
import com.dbook.infrastructure.persistence.catalog.AirportJpaEntity
import com.dbook.infrastructure.persistence.catalog.toDomain

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
