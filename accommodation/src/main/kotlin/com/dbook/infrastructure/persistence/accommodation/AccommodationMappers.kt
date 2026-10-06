// ktlint treats an import of `toDomain` as unused when the file declares one of its own: these overloads live in one
// package per concept, so the imports are needed even though they look redundant to it.
@file:Suppress("ktlint:standard:no-unused-imports")

package com.dbook.infrastructure.persistence.accommodation

import com.dbook.domain.accommodation.Accommodation
import com.dbook.domain.accommodation.RoomType
import com.dbook.infrastructure.persistence.catalog.toDomain

fun AccommodationJpaEntity.toDomain(roomTypes: List<RoomType>): Accommodation =
    Accommodation(
        id = id,
        name = title,
        destination = destination.toDomain(),
        address = address,
        stars = stars,
        description = description,
        photoUrl = photoUrl,
        amenities = amenities.split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet(),
        roomTypes = roomTypes,
        active = active,
    )
