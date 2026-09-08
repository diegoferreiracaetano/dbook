package com.dbook.infrastructure.persistence

import com.dbook.domain.Airport
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
