// ktlint treats an import of `toDomain` as unused when the file declares one of its own: these overloads live in one
// package per concept, so the imports are needed even though they look redundant to it.
@file:Suppress("ktlint:standard:no-unused-imports")

package com.dbook.infrastructure.persistence.catalog

import com.dbook.domain.catalog.Airport
import com.dbook.domain.catalog.Bookable
import com.dbook.infrastructure.persistence.accommodation.AccommodationJpaEntity
import com.dbook.infrastructure.persistence.accommodation.toDomain
import com.dbook.infrastructure.persistence.flight.FlightJpaEntity
import com.dbook.infrastructure.persistence.flight.toDomain

fun AirportJpaEntity.toDomain(): Airport =
    Airport(
        id = id,
        iataCode = iataCode,
        name = name,
        city = city,
        country = country,
        photoUrl = photoUrl,
        region = region,
        isPopular = isPopular,
    )

// Bookable is abstract: whatever arrives here at runtime is always a concrete
// specialization (today only FlightJpaEntity; Accommodation joins in M9 with a new `is`).
fun BookableJpaEntity.toDomain(availableCapacity: Int): Bookable =
    when (this) {
        is FlightJpaEntity -> this.toDomain(availableCapacity)
        // the light form (no room types): a booking only needs to know which hotel it is at
        is AccommodationJpaEntity -> this.toDomain(emptyList())
        else -> error("Unknown Bookable subtype: ${this::class}")
    }
