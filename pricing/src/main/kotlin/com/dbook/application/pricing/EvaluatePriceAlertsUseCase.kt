package com.dbook.application.pricing

import com.dbook.application.common.countOutcome
import com.dbook.domain.messaging.OutboxWriter
import com.dbook.domain.pricing.PriceAlertEvents
import com.dbook.domain.pricing.PriceAlertRepository
import com.dbook.domain.pricing.PriceChange
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.observation.annotation.Observed
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Duration

/** The same alert is not told again for this long, however the price moves. */
val PRICE_ALERT_WINDOW: Duration = Duration.ofDays(1)

/**
 * A flight got a price (the `flight.price-changed` event): every alert of its route and date that the price satisfies
 * becomes a `price-alert.triggered` event, which the notification pipeline turns into messages. The alerts are claimed
 * in one statement, so the event delivered twice, or two events together, trigger an alert once per window.
 */
@Observed(name = "dbook.usecase")
@Service
class EvaluatePriceAlertsUseCase(
    private val alerts: PriceAlertRepository,
    private val outboxWriter: OutboxWriter,
    private val meterRegistry: MeterRegistry,
    private val clock: Clock,
) {
    @Transactional
    fun execute(change: PriceChange): Int {
        val now = clock.instant()
        val triggered =
            alerts.claimTriggered(change, now, now.minus(PRICE_ALERT_WINDOW))
        triggered.forEach { outboxWriter.add(PriceAlertEvents.triggered(it, change, now)) }
        if (triggered.isNotEmpty()) {
            meterRegistry.countOutcome("dbook.price.alert", "triggered")
        }
        return triggered.size
    }
}
