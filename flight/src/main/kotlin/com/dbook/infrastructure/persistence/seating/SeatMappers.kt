package com.dbook.infrastructure.persistence.seating

import com.dbook.domain.seating.Seat
import com.dbook.infrastructure.persistence.catalog.BookableJpaEntity

fun SeatJpaEntity.toDomain(): Seat =
    Seat(
        id = id,
        bookableId = bookable.id ?: error("A persisted Seat must reference a persisted Bookable"),
        label = label,
        status = status,
    )

fun Seat.toJpaEntity(bookable: BookableJpaEntity): SeatJpaEntity =
    SeatJpaEntity(
        id = id,
        bookable = bookable,
        label = label,
        status = status,
    )
