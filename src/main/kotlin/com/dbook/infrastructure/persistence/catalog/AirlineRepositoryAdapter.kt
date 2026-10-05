package com.dbook.infrastructure.persistence.catalog

import com.dbook.domain.catalog.Airline
import com.dbook.domain.catalog.AirlineRepository
import com.dbook.domain.catalog.DuplicateIataCodeException
import com.dbook.domain.catalog.FlightSearchCache
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Repository

@Repository
class AirlineRepositoryAdapter(
    private val airlineJpaRepository: AirlineJpaRepository,
    private val searchCache: FlightSearchCache,
) : AirlineRepository {
    override fun findByIataCode(iataCode: String): Airline? = airlineJpaRepository.findByIataCode(iataCode)?.toDomain()

    override fun findById(id: Long): Airline? = airlineJpaRepository.findById(id).orElse(null)?.toDomain()

    override fun findAll(): List<Airline> = airlineJpaRepository.findAll(Sort.by("iataCode")).map { it.toDomain() }

    // flushed right away so that the unique index answers here, where it becomes a 409, and not at commit
    override fun save(airline: Airline): Airline =
        try {
            searchCache.invalidateAll()
            airlineJpaRepository.saveAndFlush(AirlineJpaEntity(airline.id, airline.iataCode, airline.name)).toDomain()
        } catch (ex: DataIntegrityViolationException) {
            throw DuplicateIataCodeException(airline.iataCode).apply { initCause(ex) }
        }

    override fun delete(id: Long) = airlineJpaRepository.deleteById(id)

    override fun flightCount(id: Long): Long = airlineJpaRepository.countFlights(id)
}
