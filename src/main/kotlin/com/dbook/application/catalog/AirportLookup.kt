package com.dbook.application.catalog

import com.dbook.domain.catalog.Airport
import com.dbook.domain.catalog.AirportNotFoundException
import com.dbook.domain.catalog.AirportRepository
import org.springframework.stereotype.Service

/** Resolves an airport by IATA code, or says that it is unknown: flights and hotels both point to airports. */
@Service
class AirportLookup(
    private val airportRepository: AirportRepository,
) {
    fun airport(iataCode: String): Airport =
        airportRepository.findByIataCode(iataCode) ?: throw AirportNotFoundException(iataCode)
}
