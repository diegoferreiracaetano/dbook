package com.dbook.infrastructure.persistence.catalog

import com.dbook.domain.catalog.Airport
import com.dbook.domain.catalog.AirportRepository
import com.dbook.domain.catalog.DuplicateIataCodeException
import com.dbook.domain.flight.FlightSearchCache
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Repository

@Repository
class AirportRepositoryAdapter(
    private val airportJpaRepository: AirportJpaRepository,
    private val searchCache: FlightSearchCache,
) : AirportRepository {
    override fun findByIataCode(iataCode: String): Airport? = airportJpaRepository.findByIataCode(iataCode)?.toDomain()

    override fun findById(id: Long): Airport? = airportJpaRepository.findById(id).orElse(null)?.toDomain()

    override fun findAll(): List<Airport> = airportJpaRepository.findAll(Sort.by("iataCode")).map { it.toDomain() }

    // flushed right away so that the unique index answers here, where it becomes a 409, and not at commit
    override fun save(airport: Airport): Airport =
        try {
            searchCache.invalidateAll()
            airportJpaRepository.saveAndFlush(
                AirportJpaEntity(
                    airport.id,
                    airport.iataCode,
                    airport.name,
                    airport.city,
                    airport.country,
                    airport.photoUrl,
                    airport.region,
                    airport.isPopular,
                ),
            ).toDomain()
        } catch (ex: DataIntegrityViolationException) {
            throw DuplicateIataCodeException(airport.iataCode).apply { initCause(ex) }
        }

    override fun delete(id: Long) = airportJpaRepository.deleteById(id)

    override fun flightCount(id: Long): Long = airportJpaRepository.countFlights(id)
}
