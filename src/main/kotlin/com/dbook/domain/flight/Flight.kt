package com.dbook.domain.flight

import com.dbook.domain.catalog.Airport
import com.dbook.domain.catalog.Bookable
import java.math.BigDecimal
import java.time.LocalDateTime

/** A [Bookable] flight: a specific route, schedule, and seat class. */
class Flight(
    id: Long? = null,
    title: String,
    price: BigDecimal,
    totalCapacity: Int,
    availableCapacity: Int,
    active: Boolean = true,
    val flightNumber: String,
    val airline: Airline,
    val origin: Airport,
    val destination: Airport,
    val departureTime: LocalDateTime,
    val arrivalTime: LocalDateTime,
    val seatClass: SeatClass,
    val aircraftType: String,
) : Bookable(id, title, price, totalCapacity, availableCapacity, active) {
    init {
        require(arrivalTime.isAfter(departureTime)) { "arrivalTime must be after departureTime" }
        require(origin.iataCode != destination.iataCode) { "origin and destination must be different airports" }
    }

    val status: FlightStatus get() = if (active) FlightStatus.SCHEDULED else FlightStatus.CANCELLED

    /** What a traveller with a booking must be told if it differs from [other]: the schedule, the route, the number. */
    fun travelChangesFrom(other: Flight): List<String> =
        listOfNotNull(
            "flightNumber".takeIf { flightNumber != other.flightNumber },
            "origin".takeIf { origin.iataCode != other.origin.iataCode },
            "destination".takeIf { destination.iataCode != other.destination.iataCode },
            "departureTime".takeIf { departureTime != other.departureTime },
            "arrivalTime".takeIf { arrivalTime != other.arrivalTime },
        )

    /** The same flight with new data. The seat map is not touched here (see `planSeatChange`). */
    fun edit(change: FlightEdit): Flight {
        check(active) { "A cancelled flight cannot be edited" }
        return Flight(
            id = id,
            title = "${change.flightNumber} ${change.origin.iataCode}-${change.destination.iataCode}",
            price = change.price,
            totalCapacity = change.totalCapacity,
            availableCapacity = change.totalCapacity,
            active = true,
            flightNumber = change.flightNumber,
            airline = change.airline,
            origin = change.origin,
            destination = change.destination,
            departureTime = change.departureTime,
            arrivalTime = change.arrivalTime,
            seatClass = change.seatClass,
            aircraftType = change.aircraftType,
        )
    }

    /** Takes the flight off sale. Whether it has bookings to deal with first is the caller's check. */
    fun cancel(): Flight {
        check(active) { "The flight is already cancelled" }
        return Flight(
            id, title, price, totalCapacity, availableCapacity, false, flightNumber, airline, origin, destination,
            departureTime, arrivalTime, seatClass, aircraftType,
        )
    }
}

data class FlightEdit(
    val flightNumber: String,
    val airline: Airline,
    val origin: Airport,
    val destination: Airport,
    val departureTime: LocalDateTime,
    val arrivalTime: LocalDateTime,
    val seatClass: SeatClass,
    val price: BigDecimal,
    val totalCapacity: Int,
    val aircraftType: String,
)
