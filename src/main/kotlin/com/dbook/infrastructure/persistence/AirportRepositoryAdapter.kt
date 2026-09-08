package com.dbook.infrastructure.persistence

import com.dbook.domain.Airport
import com.dbook.domain.AirportRepository
import org.springframework.stereotype.Repository

@Repository
class AirportRepositoryAdapter(
    private val airportJpaRepository: AirportJpaRepository,
) : AirportRepository {
    override fun findByIataCode(iataCode: String): Airport? = airportJpaRepository.findByIataCode(iataCode)?.toDomain()
}
