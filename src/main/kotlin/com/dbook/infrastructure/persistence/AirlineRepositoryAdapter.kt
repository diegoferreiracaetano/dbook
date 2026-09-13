package com.dbook.infrastructure.persistence

import com.dbook.domain.Airline
import com.dbook.domain.AirlineRepository
import org.springframework.stereotype.Repository

@Repository
class AirlineRepositoryAdapter(
    private val airlineJpaRepository: AirlineJpaRepository,
) : AirlineRepository {
    override fun findByIataCode(iataCode: String): Airline? = airlineJpaRepository.findByIataCode(iataCode)?.toDomain()
}
