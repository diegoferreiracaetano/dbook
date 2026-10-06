package com.dbook.domain.flight

import com.dbook.domain.common.PageQuery
import com.dbook.domain.common.PageResult
import java.math.BigDecimal
import java.time.LocalDateTime

data class AdminFlightFilter(
    val originIataCode: String? = null,
    val destinationIataCode: String? = null,
    val airlineIataCode: String? = null,
    val departureFrom: LocalDateTime? = null,
    val departureTo: LocalDateTime? = null,
    val status: FlightStatus? = null,
) {
    init {
        require(departureFrom == null || departureTo == null || !departureTo.isBefore(departureFrom)) {
            "departureTo must not be before departureFrom"
        }
    }
}

/** `version` is what an edit must send back, so that a stale copy is refused. */
data class AdminFlightSummary(
    val id: Long,
    val flightNumber: String,
    val airlineIataCode: String,
    val airlineName: String,
    val originIataCode: String,
    val destinationIataCode: String,
    val departureTime: LocalDateTime,
    val arrivalTime: LocalDateTime,
    val seatClass: SeatClass,
    val price: BigDecimal,
    val totalCapacity: Int,
    val availableSeats: Int,
    val reservedSeats: Int,
    val aircraftType: String,
    val status: FlightStatus,
    val version: Long,
)

data class AdminFlightDetail(
    val flight: AdminFlightSummary,
    val activeBookings: Long,
)

interface AdminFlightReader {
    /** By departure, soonest first. */
    fun search(
        filter: AdminFlightFilter,
        page: PageQuery,
    ): PageResult<AdminFlightSummary>

    fun find(id: Long): AdminFlightDetail?

    /** Whether a flight with this number already departs at this time: what makes an import repeatable. */
    fun exists(
        flightNumber: String,
        departureTime: LocalDateTime,
    ): Boolean
}
