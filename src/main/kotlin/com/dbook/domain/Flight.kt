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
    val origin: Airport,
    val destination: Airport,
    val departureTime: LocalDateTime,
    val arrivalTime: LocalDateTime,
    val seatClass: SeatClass,
) : Bookable(id, title, price, totalCapacity, availableCapacity, active)
