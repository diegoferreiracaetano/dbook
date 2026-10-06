package com.dbook.infrastructure.persistence.booking

import com.dbook.domain.booking.Booking
import com.dbook.domain.booking.Stay
import com.dbook.domain.catalog.Bookable
import com.dbook.infrastructure.persistence.catalog.BookableJpaEntity

fun BookingJpaEntity.toDomain(bookable: Bookable): Booking =
    Booking(
        id = id,
        bookable = bookable,
        seatId = seatId,
        customerId = customerId,
        status = status,
        paymentId = paymentId,
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

fun Booking.toJpaEntity(bookable: BookableJpaEntity): BookingJpaEntity =
    BookingJpaEntity(
        id = id,
        bookable = bookable,
        seatId = seatId,
        customerId = customerId,
        status = status,
        paymentId = paymentId,
        version = version,
        price = price,
        discount = discount,
        roomTypeId = stay?.roomTypeId,
        checkIn = stay?.checkIn,
        checkOut = stay?.checkOut,
        guests = stay?.guests,
        nightlyRate = stay?.nightlyRate,
    )
