package com.dbook.infrastructure.persistence

import com.dbook.domain.Airport
import com.dbook.domain.Bookable
import com.dbook.domain.Booking
import com.dbook.domain.Flight

fun AirportJpaEntity.toDomain(): Airport = Airport(
	id = id,
	iataCode = iataCode,
	name = name,
	city = city,
	country = country,
)

fun FlightJpaEntity.toDomain(): Flight = Flight(
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
fun BookableJpaEntity.toDomain(): Bookable = when (this) {
	is FlightJpaEntity -> this.toDomain()
	else -> error("Unknown Bookable subtype: ${this::class}")
}

fun BookingJpaEntity.toDomain(): Booking = Booking(
	id = id,
	bookable = bookable.toDomain(),
	customerId = customerId,
	status = status,
)
