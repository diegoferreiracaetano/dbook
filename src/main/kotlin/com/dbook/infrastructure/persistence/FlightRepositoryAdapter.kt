package com.dbook.infrastructure.persistence

import com.dbook.domain.Flight
import com.dbook.domain.FlightRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class FlightRepositoryAdapter(
	private val flightJpaRepository: FlightJpaRepository,
	private val airportJpaRepository: AirportJpaRepository,
) : FlightRepository {

	override fun findById(id: Long): Flight? =
		flightJpaRepository.findById(id).orElse(null)?.toDomain()

	override fun save(flight: Flight): Flight {
		val originRef = airportJpaRepository.getReferenceById(flight.origin.id!!)
		val destinationRef = airportJpaRepository.getReferenceById(flight.destination.id!!)
		val entity = FlightJpaEntity(
			id = flight.id,
			title = flight.title,
			price = flight.price,
			totalCapacity = flight.totalCapacity,
			availableCapacity = flight.availableCapacity,
			active = flight.active,
			flightNumber = flight.flightNumber,
			origin = originRef,
			destination = destinationRef,
			departureTime = flight.departureTime,
			arrivalTime = flight.arrivalTime,
			seatClass = flight.seatClass,
		)
		val saved = flightJpaRepository.save(entity)
		// origin/destination aqui são proxies (getReferenceById) só com o id preenchido;
		// lê-los fora da transação dispara LazyInitializationException. Reaproveitamos os
		// objetos de domínio completos que o chamador já tinha, só atualizando o id gerado.
		return Flight(
			id = saved.id,
			title = saved.title,
			price = saved.price,
			totalCapacity = saved.totalCapacity,
			availableCapacity = saved.availableCapacity,
			active = saved.active,
			flightNumber = saved.flightNumber,
			origin = flight.origin,
			destination = flight.destination,
			departureTime = saved.departureTime,
			arrivalTime = saved.arrivalTime,
			seatClass = saved.seatClass,
		)
	}

	override fun search(originIataCode: String, destinationIataCode: String, date: LocalDate): List<Flight> {
		val start = date.atStartOfDay()
		val end = date.plusDays(1).atStartOfDay()
		return flightJpaRepository
			.findByOrigin_IataCodeAndDestination_IataCodeAndDepartureTimeBetween(originIataCode, destinationIataCode, start, end)
			.map { it.toDomain() }
	}
}
