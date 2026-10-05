package com.dbook.domain.pricing

import com.dbook.domain.common.PageQuery
import com.dbook.domain.common.PageResult
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

private val MAX_TARGET = BigDecimal("1000000")

/** A customer asking to be told when a flight on a route and date gets down to [targetPrice]. */
data class PriceAlert(
    val id: Long? = null,
    val userId: Long,
    val origin: String,
    val destination: String,
    val travelDate: LocalDate,
    val targetPrice: BigDecimal,
    val active: Boolean = true,
    val lastNotifiedAt: Instant? = null,
    val createdAt: Instant,
) {
    init {
        require(isIata(origin) && isIata(destination)) { "origin and destination are 3-letter IATA codes in capitals" }
        require(origin != destination) { "origin and destination must be different" }
        require(targetPrice > BigDecimal.ZERO && targetPrice <= MAX_TARGET) {
            "targetPrice must be positive and at most $MAX_TARGET"
        }
    }

    private fun isIata(code: String) = code.length == IATA_LENGTH && code.all { it in 'A'..'Z' }

    private companion object {
        const val IATA_LENGTH = 3
    }
}

/** An alert that a price event matched and claimed, with what the message needs. */
data class TriggeredAlert(
    val alertId: Long,
    val userId: Long,
    val targetPrice: BigDecimal,
)

class PriceAlertNotFoundException(id: Long) : RuntimeException("Price alert not found: $id")

class DuplicatePriceAlertException : RuntimeException("You already have an alert for this route and date")

class PriceAlertsLimitReachedException(val limit: Int) :
    RuntimeException("You can have at most $limit active price alerts; remove one first")

interface PriceAlertRepository {
    /**
     * Creates the alert unless the customer already has [limit] active alerts for dates from [today] on (null is
     * returned), atomically. @throws DuplicatePriceAlertException for the same route and date.
     */
    fun create(
        alert: PriceAlert,
        limit: Int,
        today: LocalDate,
    ): PriceAlert?

    fun findById(id: Long): PriceAlert?

    fun save(alert: PriceAlert): PriceAlert

    fun delete(id: Long)

    /** The customer's alerts, newest first. */
    fun findByUser(
        userId: Long,
        page: PageQuery,
    ): PageResult<PriceAlert>

    /**
     * The alerts that the flight of [change] (its route, date and price) satisfies and that were not notified since
     * [notifiedBefore]: **claimed in one statement** (their last notification is set to [now]), so the same event
     * delivered twice, or two events at once, can never trigger an alert twice inside the window.
     */
    fun claimTriggered(
        change: PriceChange,
        now: Instant,
        notifiedBefore: Instant,
    ): List<TriggeredAlert>
}
