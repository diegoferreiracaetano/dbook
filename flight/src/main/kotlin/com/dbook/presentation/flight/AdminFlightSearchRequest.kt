package com.dbook.presentation.flight

import com.dbook.domain.common.PageQuery
import com.dbook.domain.flight.AdminFlightFilter
import com.dbook.domain.flight.FlightStatus
import java.time.LocalDateTime

/** The query string of `GET /admin/flights`: every filter is optional. Times look like 2026-10-01T08:00:00. */
data class AdminFlightSearchRequest(
    val origin: String? = null,
    val destination: String? = null,
    val airline: String? = null,
    val departureFrom: LocalDateTime? = null,
    val departureTo: LocalDateTime? = null,
    val status: FlightStatus? = null,
    val page: Int = 0,
    val size: Int = PageQuery.DEFAULT_SIZE,
) {
    fun toFilter() =
        AdminFlightFilter(
            originIataCode = origin?.uppercase(),
            destinationIataCode = destination?.uppercase(),
            airlineIataCode = airline?.uppercase(),
            departureFrom = departureFrom,
            departureTo = departureTo,
            status = status,
        )

    fun toPage() = PageQuery(page, size)
}
