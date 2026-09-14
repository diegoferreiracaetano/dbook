package com.dbook.infrastructure.persistence

import com.dbook.domain.Flight
import com.dbook.domain.FlightRepository
import com.dbook.domain.SeatStatus
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDate

@Repository
class FlightRepositoryAdapter(
    private val flightJpaRepository: FlightJpaRepository,
    private val airlineJpaRepository: AirlineJpaRepository,
    private val airportJpaRepository: AirportJpaRepository,
    private val seatJpaRepository: SeatJpaRepository,
) : FlightRepository {
    override fun findById(id: Long): Flight? =
        flightJpaRepository.findById(id).orElse(null)?.toDomain(availableCapacityOf(id))

    override fun save(flight: Flight): Flight {
        val airlineId = requireNotNull(flight.airline.id) { "Flight.airline must be a persisted Airline" }
        val originId = requireNotNull(flight.origin.id) { "Flight.origin must be a persisted Airport" }
        val destinationId = requireNotNull(flight.destination.id) { "Flight.destination must be a persisted Airport" }
        val airlineRef = airlineJpaRepository.getReferenceById(airlineId)
        val originRef = airportJpaRepository.getReferenceById(originId)
        val destinationRef = airportJpaRepository.getReferenceById(destinationId)
        val saved = flightJpaRepository.save(flight.toJpaEntity(airlineRef, originRef, destinationRef))
        // origin/destination here are proxies (getReferenceById) with only the id set;
        // reading them outside the transaction throws LazyInitializationException. We
        // reuse the full domain objects the caller already had, only refreshing the
        // generated id. availableCapacity reflects the seats persisted so far for this
        // bookable (none yet, right after this insert — the caller generates them next).
        return Flight(
            id = saved.id,
            title = saved.title,
            price = saved.price,
            totalCapacity = saved.totalCapacity,
            availableCapacity = availableCapacityOf(requireNotNull(saved.id)),
            active = saved.active,
            flightNumber = saved.flightNumber,
            airline = flight.airline,
            origin = flight.origin,
            destination = flight.destination,
            departureTime = saved.departureTime,
            arrivalTime = saved.arrivalTime,
            seatClass = saved.seatClass,
            aircraftType = saved.aircraftType,
        )
    }

    override fun search(
        originIataCode: String,
        destinationIataCode: String,
        date: LocalDate,
    ): List<Flight> {
        val start = date.atStartOfDay()
        val end = date.plusDays(1).atStartOfDay()
        return flightJpaRepository
            .findByOrigin_IataCodeAndDestination_IataCodeAndDepartureTimeBetween(
                originIataCode,
                destinationIataCode,
                start,
                end,
            )
            .map { it.toDomain(availableCapacityOf(requireNotNull(it.id))) }
    }

    override fun findActive(): List<Flight> =
        flightJpaRepository.findTop50ByActiveTrueOrderByDepartureTimeAsc()
            .map { it.toDomain(availableCapacityOf(requireNotNull(it.id))) }

    override fun findLowestPrice(
        destinationIataCode: String,
        from: LocalDate,
        to: LocalDate,
    ): BigDecimal? =
        flightJpaRepository.findLowestPrice(
            destinationIataCode,
            from.atStartOfDay(),
            to.plusDays(1).atStartOfDay(),
        )

    private fun availableCapacityOf(bookableId: Long): Int =
        seatJpaRepository.countByBookable_IdAndStatus(bookableId, SeatStatus.AVAILABLE)
}
