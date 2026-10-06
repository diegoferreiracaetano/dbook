package com.dbook.domain.flight

import com.dbook.domain.messaging.OutboxEvent
import java.time.Instant

object FlightEvents {
    const val CHANGED = "flight.changed"
    const val PRICE_CHANGED = "flight.price-changed"

    /** The flight has [Flight.price] from now on; [previous] is null when it was just created. */
    fun priceChanged(
        flight: Flight,
        previous: java.math.BigDecimal?,
        at: Instant,
    ) = OutboxEvent(
        "flight",
        requireNotNull(flight.id).toString(),
        PRICE_CHANGED,
        mapOf(
            "flightId" to flight.id,
            "flightNumber" to flight.flightNumber,
            "origin" to flight.origin.iataCode,
            "destination" to flight.destination.iataCode,
            "date" to flight.departureTime.toLocalDate().toString(),
            "price" to cents(flight.price),
            "previousPrice" to previous?.let(::cents),
        ),
        at,
    )

    /** One event per booking affected: [changes] names what moved (e.g. `departureTime`), [flight] is how it is now. */
    fun changed(
        flight: Flight,
        bookingId: Long,
        customerId: Long,
        changes: List<String>,
        at: Instant,
    ) = OutboxEvent(
        "flight",
        requireNotNull(flight.id).toString(),
        CHANGED,
        mapOf(
            "bookingId" to bookingId,
            "customerId" to customerId,
            "flightNumber" to flight.flightNumber,
            "title" to flight.title,
            "changes" to changes,
            "departureTime" to flight.departureTime.toString(),
        ),
        at,
    )

    // money in events always has its cents, whatever scale the number came with
    private fun cents(amount: java.math.BigDecimal): String =
        amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()
}
