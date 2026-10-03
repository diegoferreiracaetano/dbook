package com.dbook.infrastructure.persistence.booking

import com.dbook.domain.booking.Booking
import com.dbook.infrastructure.persistence.catalog.BookableJpaEntity
import com.dbook.infrastructure.persistence.catalog.toDomain
import com.dbook.infrastructure.persistence.payment.PaymentJpaEntity
import com.dbook.infrastructure.persistence.seating.SeatJpaEntity

fun BookingJpaEntity.toDomain(availableCapacity: Int): Booking =
    Booking(
        id = id,
        bookable = bookable.toDomain(availableCapacity),
        seatId = seat.id ?: error("A persisted Booking must reference a persisted Seat"),
        customerId = customerId,
        status = status,
        paymentId = payment?.id,
        version = version,
    )

fun Booking.toJpaEntity(
    bookable: BookableJpaEntity,
    seat: SeatJpaEntity,
    payment: PaymentJpaEntity?,
): BookingJpaEntity =
    BookingJpaEntity(
        id = id,
        bookable = bookable,
        seat = seat,
        customerId = customerId,
        status = status,
        payment = payment,
        version = version,
    )
