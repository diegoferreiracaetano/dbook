package com.dbook.infrastructure.persistence.booking

import com.dbook.domain.booking.Booking
import com.dbook.domain.booking.Stay
import com.dbook.infrastructure.persistence.catalog.BookableJpaEntity
import com.dbook.infrastructure.persistence.catalog.toDomain
import com.dbook.infrastructure.persistence.payment.PaymentJpaEntity
import com.dbook.infrastructure.persistence.seating.SeatJpaEntity

fun BookingJpaEntity.toDomain(availableCapacity: Int): Booking =
    Booking(
        id = id,
        bookable = bookable.toDomain(availableCapacity),
        seatId = seat?.let { it.id ?: error("A persisted Booking must reference a persisted Seat") },
        customerId = customerId,
        status = status,
        paymentId = payment?.id,
        version = version,
        price = price,
        discount = discount,
        stay = stay(),
    )

private fun BookingJpaEntity.stay(): Stay? =
    roomTypeId?.let {
        Stay(
            it,
            requireNotNull(checkIn),
            requireNotNull(checkOut),
            requireNotNull(guests),
            requireNotNull(nightlyRate),
        )
    }

fun Booking.toJpaEntity(
    bookable: BookableJpaEntity,
    seat: SeatJpaEntity?,
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
        price = price,
        discount = discount,
        roomTypeId = stay?.roomTypeId,
        checkIn = stay?.checkIn,
        checkOut = stay?.checkOut,
        guests = stay?.guests,
        nightlyRate = stay?.nightlyRate,
    )
