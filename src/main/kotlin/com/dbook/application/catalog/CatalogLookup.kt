package com.dbook.application.catalog

import com.dbook.domain.catalog.Airline
import com.dbook.domain.catalog.AirlineNotFoundException
import com.dbook.domain.catalog.AirlineRepository
import com.dbook.domain.catalog.Airport
import com.dbook.domain.catalog.AirportNotFoundException
import com.dbook.domain.catalog.AirportRepository
import org.springframework.stereotype.Service

/** Resolves an airline or an airport by IATA code, or says which one is unknown. */
@Service
class CatalogLookup(
    private val airlineRepository: AirlineRepository,
    private val airportRepository: AirportRepository,
) {
    fun airline(iataCode: String): Airline =
        airlineRepository.findByIataCode(iataCode) ?: throw AirlineNotFoundException(iataCode)

    fun airport(iataCode: String): Airport =
        airportRepository.findByIataCode(iataCode) ?: throw AirportNotFoundException(iataCode)
}
