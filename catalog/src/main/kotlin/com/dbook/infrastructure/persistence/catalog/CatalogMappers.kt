package com.dbook.infrastructure.persistence.catalog

import com.dbook.domain.catalog.Airport

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
