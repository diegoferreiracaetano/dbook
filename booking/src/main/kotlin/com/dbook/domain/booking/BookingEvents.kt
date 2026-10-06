package com.dbook.domain.booking

import com.dbook.domain.messaging.OutboxEvent
import java.time.Instant

/** The events a booking produces, as the outbox carries them. */
private const val HOLD_MINUTES = 15L

object BookingEvents {
    /** How long a PENDING booking holds what it reserved (a seat, the nights of a stay) before it expires. */
    val HOLD: java.time.Duration = java.time.Duration.ofMinutes(HOLD_MINUTES)

    const val EXPIRATION_REQUESTED = "booking.expiration.requested"
    const val CONFIRMED = "booking.confirmed"
    const val EXPIRED = "booking.expired"
    const val CANCELLED_BY_STAFF = "booking.cancelled-by-staff"

    /** The booking must be looked at again at [dueAt]: if it is still PENDING by then, it expires. */
    fun expirationRequested(
        bookingId: Long,
        dueAt: Instant,
    ) = OutboxEvent("booking", bookingId.toString(), EXPIRATION_REQUESTED, mapOf("bookingId" to bookingId), dueAt)

    fun confirmed(
        booking: Booking,
        at: Instant,
    ) = of(CONFIRMED, booking, at)

    fun expired(
        booking: Booking,
        at: Instant,
    ) = of(EXPIRED, booking, at)

    fun cancelledByStaff(
        booking: Booking,
        at: Instant,
    ) = of(CANCELLED_BY_STAFF, booking, at)

    // The payload is what a notification needs to say something useful without asking anyone again.
    private fun of(
        type: String,
        booking: Booking,
        at: Instant,
    ) = OutboxEvent(
        "booking",
        requireNotNull(booking.id).toString(),
        type,
        mapOf(
            "bookingId" to booking.id,
            "customerId" to booking.customerId,
            "title" to booking.bookable.title,
            "price" to booking.price.toPlainString(),
            "departureTime" to booking.bookable.startsAt?.toString(),
        ),
        at,
    )
}
