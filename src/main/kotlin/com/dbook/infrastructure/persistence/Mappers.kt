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

// Bookable é abstrata: quem chega aqui em runtime é sempre uma especialização concreta
// (hoje só FlightJpaEntity; Accommodation entra no M9 com um novo `is`).
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
