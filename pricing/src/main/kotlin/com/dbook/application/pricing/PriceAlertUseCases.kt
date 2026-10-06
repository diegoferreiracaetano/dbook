package com.dbook.application.pricing

import com.dbook.domain.catalog.AirportNotFoundException
import com.dbook.domain.catalog.AirportRepository
import com.dbook.domain.common.PageQuery
import com.dbook.domain.common.PageResult
import com.dbook.domain.pricing.PriceAlert
import com.dbook.domain.pricing.PriceAlertNotFoundException
import com.dbook.domain.pricing.PriceAlertRepository
import com.dbook.domain.pricing.PriceAlertsLimitReachedException
import io.micrometer.observation.annotation.Observed
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate

/** The most active alerts (for dates still ahead) a customer can have. */
const val MAX_PRICE_ALERTS_PER_USER = 20

data class CreatePriceAlertCommand(
    val userId: Long,
    val origin: String,
    val destination: String,
    val date: LocalDate,
    val targetPrice: BigDecimal,
)

/**
 * Creates an alert for a route and a date. Both airports must exist (404), the date cannot be in the past, there is
 * one alert per route and date (a second is a 409, change the target instead) and a limit per customer (409).
 */
@Observed(name = "dbook.usecase")
@Service
class CreatePriceAlertUseCase(
    private val alerts: PriceAlertRepository,
    private val airports: AirportRepository,
    private val clock: Clock,
) {
    @Transactional
    fun execute(command: CreatePriceAlertCommand): PriceAlert {
        val today = LocalDate.now(clock)
        require(!command.date.isBefore(today)) { "date must not be in the past" }
        val alert =
            PriceAlert(
                userId = command.userId,
                origin = command.origin,
                destination = command.destination,
                travelDate = command.date,
                targetPrice = command.targetPrice,
                createdAt = clock.instant(),
            )
        listOf(
            alert.origin,
            alert.destination,
        ).forEach { airports.findByIataCode(it) ?: throw AirportNotFoundException(it) }
        return alerts.create(alert, MAX_PRICE_ALERTS_PER_USER, today)
            ?: throw PriceAlertsLimitReachedException(MAX_PRICE_ALERTS_PER_USER)
    }
}

@Observed(name = "dbook.usecase")
@Service
class ListPriceAlertsUseCase(
    private val alerts: PriceAlertRepository,
) {
    fun execute(
        userId: Long,
        page: PageQuery,
    ): PageResult<PriceAlert> = alerts.findByUser(userId, page)
}

data class UpdatePriceAlertCommand(
    val userId: Long,
    val alertId: Long,
    val targetPrice: BigDecimal?,
    val active: Boolean?,
)

/** Changes the target, switches the alert off or on, or both. Someone else's alert looks like one that is not there. */
@Observed(name = "dbook.usecase")
@Service
class UpdatePriceAlertUseCase(
    private val alerts: PriceAlertRepository,
) {
    @Transactional
    fun execute(command: UpdatePriceAlertCommand): PriceAlert {
        require(command.targetPrice != null || command.active != null) { "send a targetPrice, active or both" }
        val alert = mine(alerts, command.userId, command.alertId)
        val changed =
            alert.copy(
                targetPrice = command.targetPrice ?: alert.targetPrice,
                active = command.active ?: alert.active,
                // a new target is a new promise: the next price that meets it is worth telling, whatever came before
                lastNotifiedAt = if (command.targetPrice != null) null else alert.lastNotifiedAt,
            )
        return alerts.save(changed)
    }
}

@Observed(name = "dbook.usecase")
@Service
class DeletePriceAlertUseCase(
    private val alerts: PriceAlertRepository,
) {
    @Transactional
    fun execute(
        userId: Long,
        alertId: Long,
    ) {
        mine(alerts, userId, alertId)
        alerts.delete(alertId)
    }
}

private fun mine(
    alerts: PriceAlertRepository,
    userId: Long,
    id: Long,
): PriceAlert = alerts.findById(id)?.takeIf { it.userId == userId } ?: throw PriceAlertNotFoundException(id)
