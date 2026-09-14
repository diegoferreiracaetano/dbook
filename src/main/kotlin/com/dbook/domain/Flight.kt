package com.dbook.domain

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
) : Bookable(id, title, price, totalCapacity, availableCapacity, active)
