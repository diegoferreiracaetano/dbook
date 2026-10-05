package com.dbook.infrastructure.persistence.catalog

import com.dbook.domain.catalog.Flight
import com.dbook.domain.catalog.FlightNotFoundException
import com.dbook.domain.catalog.FlightRepository
import com.dbook.domain.catalog.FlightSearchCache
import com.dbook.domain.common.StaleVersionException
import com.dbook.domain.seating.SeatAvailability
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate

@Repository
class FlightRepositoryAdapter(
    private val flightJpaRepository: FlightJpaRepository,
    private val airlineJpaRepository: AirlineJpaRepository,
    private val airportJpaRepository: AirportJpaRepository,
    private val seatAvailability: SeatAvailability,
    private val searchCache: FlightSearchCache,
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
        searchCache.invalidateAll()
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

    @Transactional
    override fun update(
        flight: Flight,
        expectedVersion: Long?,
    ): Flight {
        val id = requireNotNull(flight.id) { "Only a persisted Flight can be updated" }
        val entity = flightJpaRepository.findById(id).orElseThrow { FlightNotFoundException(id) }
        if (expectedVersion != null && entity.version != expectedVersion) {
            throw StaleVersionException("The flight was changed by someone else: reload it and edit again")
        }
        entity.title = flight.title
        entity.price = flight.price
        entity.totalCapacity = flight.totalCapacity
        entity.active = flight.active
        entity.flightNumber = flight.flightNumber
        entity.airline = airlineJpaRepository.getReferenceById(requireNotNull(flight.airline.id))
        entity.origin = airportJpaRepository.getReferenceById(requireNotNull(flight.origin.id))
        entity.destination = airportJpaRepository.getReferenceById(requireNotNull(flight.destination.id))
        entity.departureTime = flight.departureTime
        entity.arrivalTime = flight.arrivalTime
        entity.seatClass = flight.seatClass
        entity.aircraftType = flight.aircraftType
        // flushed now: the version moves and a concurrent change shows up here, not at some later commit
        flightJpaRepository.saveAndFlush(entity)
        searchCache.invalidateAll()
        return flight
    }

    override fun search(
        originIataCode: String,
        destinationIataCode: String,
        date: LocalDate,
    ): List<Flight> {
        val start = date.atStartOfDay()
        val end = date.plusDays(1).atStartOfDay()
        return withAvailability(
            flightJpaRepository
                .findByOrigin_IataCodeAndDestination_IataCodeAndDepartureTimeBetweenAndActiveTrue(
                    originIataCode,
                    destinationIataCode,
                    start,
                    end,
                ),
        )
    }

    override fun findActive(): List<Flight> =
        withAvailability(flightJpaRepository.findTop50ByActiveTrueOrderByDepartureTimeAsc())

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

    private fun availableCapacityOf(bookableId: Long): Int = seatAvailability.availableSeats(bookableId)

    // the free seats of every flight of a list in one query, not one query each
    private fun withAvailability(entities: List<FlightJpaEntity>): List<Flight> {
        val free = seatAvailability.availableSeatsOf(entities.map { requireNotNull(it.id) })
        return entities.map { it.toDomain(free[it.id] ?: 0) }
    }
}
