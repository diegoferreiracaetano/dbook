package com.dbook.application.flight

import com.dbook.domain.flight.FlightRepository
import io.micrometer.observation.annotation.Observed
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate

private const val LOOKAHEAD_DAYS = 60L

/**
 * Lowest real price found among active flights to a destination in the near future — no
 * authentication required. Backs the "from $X" price shown on destination cards, without the
 * mobile client guessing at nearby dates one `GET /flights/search` call at a time.
 */
@Observed(name = "dbook.usecase")
@Service
class GetLowestPriceForDestinationUseCase(
    private val flightRepository: FlightRepository,
    private val clock: Clock,
) {
    fun execute(destinationIataCode: String): BigDecimal? {
        val today = LocalDate.now(clock)
        return flightRepository.findLowestPrice(destinationIataCode, today, today.plusDays(LOOKAHEAD_DAYS))
    }
}
