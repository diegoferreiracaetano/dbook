package com.dbook.infrastructure.persistence.catalog

import com.dbook.domain.catalog.Airport
import com.dbook.domain.catalog.AirportRepository
import org.springframework.stereotype.Repository

@Repository
class AirportRepositoryAdapter(
    private val airportJpaRepository: AirportJpaRepository,
) : AirportRepository {
    override fun findByIataCode(iataCode: String): Airport? = airportJpaRepository.findByIataCode(iataCode)?.toDomain()

    override fun findAll(): List<Airport> = airportJpaRepository.findAll().map { it.toDomain() }
}
