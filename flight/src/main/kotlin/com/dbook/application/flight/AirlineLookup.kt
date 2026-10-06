package com.dbook.application.flight

import com.dbook.domain.flight.Airline
import com.dbook.domain.flight.AirlineNotFoundException
import com.dbook.domain.flight.AirlineRepository
import org.springframework.stereotype.Service

/** Resolves an airline by IATA code, or says that it is unknown. */
@Service
class AirlineLookup(
    private val airlineRepository: AirlineRepository,
) {
    fun airline(iataCode: String): Airline =
        airlineRepository.findByIataCode(iataCode) ?: throw AirlineNotFoundException(iataCode)
}
