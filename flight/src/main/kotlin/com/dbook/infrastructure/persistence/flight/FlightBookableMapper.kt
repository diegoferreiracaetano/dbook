package com.dbook.infrastructure.persistence.flight

import com.dbook.domain.catalog.Bookable
import com.dbook.domain.seating.SeatAvailability
import com.dbook.infrastructure.persistence.catalog.BookableEntityMapper
import com.dbook.infrastructure.persistence.catalog.BookableJpaEntity
import org.springframework.stereotype.Component

/** A flight is a `Bookable` whose free capacity is the number of its seats that are still available. */
@Component
class FlightBookableMapper(
    private val seatAvailability: SeatAvailability,
) : BookableEntityMapper {
    override fun supports(entity: BookableJpaEntity): Boolean = entity is FlightJpaEntity

    override fun toDomain(entities: List<BookableJpaEntity>): List<Bookable> {
        val flights = entities.map { it as FlightJpaEntity }
        val free = seatAvailability.availableSeatsOf(flights.map { requireNotNull(it.id) })
        return flights.map { it.toDomain(free[it.id] ?: 0) }
    }
}
