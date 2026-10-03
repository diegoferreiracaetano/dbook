package com.dbook.application.catalog

import com.dbook.domain.catalog.Flight
import com.dbook.domain.catalog.FlightRepository
import io.micrometer.observation.annotation.Observed
import org.springframework.stereotype.Service
import java.time.LocalDate

/** Public flight search by route and departure date — no authentication required. */
@Observed(name = "dbook.usecase")
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
