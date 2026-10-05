package com.dbook.domain.pricing

import com.dbook.domain.messaging.OutboxEvent
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

object PriceAlertEvents {
    const val TRIGGERED = "price-alert.triggered"

    fun triggered(
        alert: TriggeredAlert,
        change: PriceChange,
        at: Instant,
    ) = OutboxEvent(
        "price-alert",
        alert.alertId.toString(),
        TRIGGERED,
        mapOf(
            "alertId" to alert.alertId,
            "customerId" to alert.userId,
            "flightId" to change.flightId,
            "flightNumber" to change.flightNumber,
            "title" to "${change.origin}-${change.destination}",
            "date" to change.travelDate.toString(),
            "price" to change.price.toPlainString(),
            "targetPrice" to alert.targetPrice.toPlainString(),
        ),
        at,
    )
}

/** A flight got this price: the fact the evaluator reads from the `flight.price-changed` event. */
data class PriceChange(
    val flightId: Long,
    val flightNumber: String,
    val origin: String,
    val destination: String,
    val travelDate: LocalDate,
    val price: BigDecimal,
)
