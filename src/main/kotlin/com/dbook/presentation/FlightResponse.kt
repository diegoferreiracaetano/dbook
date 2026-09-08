package com.dbook.presentation

import com.dbook.domain.Flight
import com.dbook.domain.SeatClass
import java.math.BigDecimal
import java.time.LocalDateTime

data class FlightResponse(
    val id: Long?,
    val flightNumber: String,
    val origin: String,
    val destination: String,
    val departureTime: LocalDateTime,
    val arrivalTime: LocalDateTime,
    val seatClass: SeatClass,
    val price: BigDecimal,
    val availableCapacity: Int,
) {
    companion object {
        fun from(flight: Flight) =
            FlightResponse(
                id = flight.id,
                flightNumber = flight.flightNumber,
                origin = flight.origin.iataCode,
                destination = flight.destination.iataCode,
                departureTime = flight.departureTime,
                arrivalTime = flight.arrivalTime,
                seatClass = flight.seatClass,
                price = flight.price,
                availableCapacity = flight.availableCapacity,
            )
    }
}
