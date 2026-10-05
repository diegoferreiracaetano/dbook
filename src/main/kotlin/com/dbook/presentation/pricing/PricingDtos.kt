package com.dbook.presentation.pricing

import com.dbook.application.pricing.FlightPriceHistory
import com.dbook.domain.pricing.PriceAlert
import com.dbook.domain.pricing.PricePoint
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

data class PricePointResponse(
    val price: BigDecimal,
    val changedAt: Instant,
) {
    companion object {
        fun from(point: PricePoint) = PricePointResponse(point.price, point.changedAt)
    }
}

data class PriceHistoryResponse(
    val flightId: Long,
    val current: BigDecimal,
    val lowest: BigDecimal,
    val highest: BigDecimal,
    val points: List<PricePointResponse>,
) {
    companion object {
        fun from(history: FlightPriceHistory) =
            PriceHistoryResponse(
                history.flightId,
                history.current,
                history.lowest,
                history.highest,
                history.points.map(PricePointResponse::from),
            )
    }
}

data class CreatePriceAlertRequest(
    @get:Schema(example = "GRU", description = "origin IATA code, in capitals")
    val origin: String,
    @get:Schema(example = "GIG", description = "destination IATA code, in capitals")
    val destination: String,
    @get:Schema(example = "2027-01-15", description = "the travel date; not in the past")
    val date: LocalDate,
    @get:Schema(example = "300.00", description = "tell me when a flight costs at most this")
    val targetPrice: BigDecimal,
)

data class UpdatePriceAlertRequest(
    @get:Schema(example = "250.00", description = "a new target; leave out to keep it")
    val targetPrice: BigDecimal? = null,
    @get:Schema(example = "false", description = "switch the alert off or on; leave out to keep it")
    val active: Boolean? = null,
)

data class PriceAlertResponse(
    val id: Long?,
    val origin: String,
    val destination: String,
    val date: LocalDate,
    val targetPrice: BigDecimal,
    val active: Boolean,
    val lastNotifiedAt: Instant?,
    val createdAt: Instant,
) {
    companion object {
        fun from(alert: PriceAlert) =
            PriceAlertResponse(
                alert.id,
                alert.origin,
                alert.destination,
                alert.travelDate,
                alert.targetPrice,
                alert.active,
                alert.lastNotifiedAt,
                alert.createdAt,
            )
    }
}
