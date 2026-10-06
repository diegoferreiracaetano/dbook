package com.dbook.presentation.flight

import com.dbook.domain.flight.AdminFlightDetail
import com.dbook.domain.flight.AdminFlightSummary
import com.dbook.domain.flight.FlightStatus
import com.dbook.domain.flight.SeatClass
import com.dbook.domain.seating.seatLayoutFor
import java.math.BigDecimal
import java.time.LocalDateTime

/** `version` goes back in the body of the edit (`PUT`): a flight someone else changed in the meantime is refused. */
data class AdminFlightSummaryResponse(
    val id: Long,
    val flightNumber: String,
    val airlineIataCode: String,
    val airlineName: String,
    val origin: String,
    val destination: String,
    val departureTime: LocalDateTime,
    val arrivalTime: LocalDateTime,
    val seatClass: SeatClass,
    val price: BigDecimal,
    val totalCapacity: Int,
    val availableSeats: Int,
    val reservedSeats: Int,
    val aircraftType: String,
    val seatLayout: List<Int>,
    val status: FlightStatus,
    val version: Long,
) {
    companion object {
        fun from(flight: AdminFlightSummary) =
            AdminFlightSummaryResponse(
                id = flight.id,
                flightNumber = flight.flightNumber,
                airlineIataCode = flight.airlineIataCode,
                airlineName = flight.airlineName,
                origin = flight.originIataCode,
                destination = flight.destinationIataCode,
                departureTime = flight.departureTime,
                arrivalTime = flight.arrivalTime,
                seatClass = flight.seatClass,
                price = flight.price,
                totalCapacity = flight.totalCapacity,
                availableSeats = flight.availableSeats,
                reservedSeats = flight.reservedSeats,
                aircraftType = flight.aircraftType,
                seatLayout = seatLayoutFor(flight.aircraftType),
                status = flight.status,
                version = flight.version,
            )
    }
}

data class AdminFlightDetailResponse(
    val flight: AdminFlightSummaryResponse,
    val activeBookings: Long,
) {
    companion object {
        fun from(detail: AdminFlightDetail) =
            AdminFlightDetailResponse(AdminFlightSummaryResponse.from(detail.flight), detail.activeBookings)
    }
}
