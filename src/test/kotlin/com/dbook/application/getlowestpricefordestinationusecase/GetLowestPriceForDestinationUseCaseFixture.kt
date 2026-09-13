package com.dbook.application.getlowestpricefordestinationusecase

import com.dbook.domain.Flight
import com.dbook.domain.FlightRepository
import java.math.BigDecimal
import java.time.LocalDate

const val LOOKAHEAD_DAYS = 60L

// A single stubbed return value is enough here: this use case is a thin pass-through, so each
// scenario only cares about what the repository returns and what window it was asked about.
class FakeFlightRepository(private val lowestPrice: BigDecimal?) : FlightRepository {
    var lastDestination: String? = null
    var lastFrom: LocalDate? = null
    var lastTo: LocalDate? = null

    override fun findById(id: Long): Flight = error("not used by this use case")

    override fun save(flight: Flight): Flight = error("not used by this use case")

    override fun search(
        originIataCode: String,
        destinationIataCode: String,
        date: LocalDate,
    ): List<Flight> = error("not used by this use case")

    override fun findActive(): List<Flight> = error("not used by this use case")

    override fun findLowestPrice(
        destinationIataCode: String,
        from: LocalDate,
        to: LocalDate,
    ): BigDecimal? {
        lastDestination = destinationIataCode
        lastFrom = from
        lastTo = to
        return lowestPrice
    }
}
