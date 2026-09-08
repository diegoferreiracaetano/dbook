package com.dbook.domain

import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FlightTest {

	private val origin = Airport(id = 1, iataCode = "GRU", name = "Guarulhos", city = "São Paulo", country = "Brasil")
	private val destination = Airport(id = 2, iataCode = "GIG", name = "Galeão", city = "Rio de Janeiro", country = "Brasil")

	private fun buildFlight(
		price: BigDecimal = BigDecimal("500.00"),
		totalCapacity: Int = 180,
		availableCapacity: Int = 180,
	) = Flight(
		title = "GRU-GIG",
		price = price,
		totalCapacity = totalCapacity,
		availableCapacity = availableCapacity,
		flightNumber = "DB1234",
		origin = origin,
		destination = destination,
		departureTime = LocalDateTime.of(2026, 10, 1, 8, 0),
		arrivalTime = LocalDateTime.of(2026, 10, 1, 9, 10),
		seatClass = SeatClass.ECONOMY,
	)

	@Test
	fun `creates a valid flight`() {
		val flight = buildFlight()
		assertEquals(180, flight.availableCapacity)
	}

	@Test
	fun `rejects negative price`() {
		assertFailsWith<IllegalArgumentException> {
			buildFlight(price = BigDecimal("-1.00"))
		}
	}

	@Test
	fun `rejects availableCapacity greater than totalCapacity`() {
		assertFailsWith<IllegalArgumentException> {
			buildFlight(totalCapacity = 100, availableCapacity = 101)
		}
	}

	@Test
	fun `rejects negative availableCapacity`() {
		assertFailsWith<IllegalArgumentException> {
			buildFlight(availableCapacity = -1)
		}
	}
}
