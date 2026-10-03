package com.dbook.application.catalog

import com.dbook.domain.catalog.Flight
import com.dbook.domain.catalog.FlightRepository
import org.springframework.stereotype.Service
import java.time.LocalDate

/** Public flight search by route and departure date — no authentication required. */
@Service
class SearchFlightsUseCase(
    private val flightRepository: FlightRepository,
) {
    fun execute(
        originIataCode: String,
        destinationIataCode: String,
        date: LocalDate,
    ): List<Flight> = flightRepository.search(originIataCode, destinationIataCode, date)
}
