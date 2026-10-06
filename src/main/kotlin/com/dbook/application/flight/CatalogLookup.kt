package com.dbook.application.flight

import com.dbook.domain.catalog.Airport
import com.dbook.domain.catalog.AirportNotFoundException
import com.dbook.domain.catalog.AirportRepository
import com.dbook.domain.flight.Airline
import com.dbook.domain.flight.AirlineNotFoundException
import com.dbook.domain.flight.AirlineRepository
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
