package com.dbook.application

import com.dbook.domain.Flight
import com.dbook.domain.FlightRepository
import org.springframework.stereotype.Service
import java.time.LocalDate

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
