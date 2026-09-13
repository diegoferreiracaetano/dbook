package com.dbook.application

import com.dbook.domain.FlightRepository
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate

private const val LOOKAHEAD_DAYS = 60L

/**
 * Lowest real price found among active flights to a destination in the near future — no
 * authentication required. Backs the "from $X" price shown on destination cards, without the
 * mobile client guessing at nearby dates one `GET /flights/search` call at a time.
 */
@Service
class GetLowestPriceForDestinationUseCase(
    private val flightRepository: FlightRepository,
) {
    fun execute(destinationIataCode: String): BigDecimal? {
        val today = LocalDate.now()
        return flightRepository.findLowestPrice(destinationIataCode, today, today.plusDays(LOOKAHEAD_DAYS))
    }
}
