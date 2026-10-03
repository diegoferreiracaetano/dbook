package com.dbook.infrastructure.persistence.catalog

import com.dbook.domain.catalog.Airline
import com.dbook.domain.catalog.AirlineRepository
import org.springframework.stereotype.Repository

@Repository
class AirlineRepositoryAdapter(
    private val airlineJpaRepository: AirlineJpaRepository,
) : AirlineRepository {
    override fun findByIataCode(iataCode: String): Airline? = airlineJpaRepository.findByIataCode(iataCode)?.toDomain()
}
