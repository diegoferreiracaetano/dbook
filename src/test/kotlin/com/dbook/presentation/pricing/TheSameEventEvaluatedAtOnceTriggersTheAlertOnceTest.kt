package com.dbook.presentation.pricing

import com.dbook.application.pricing.EvaluatePriceAlertsUseCase
import com.dbook.domain.pricing.PriceChange
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import kotlin.test.Test
import kotlin.test.assertEquals

class TheSameEventEvaluatedAtOnceTriggersTheAlertOnceTest : PricingApiFixture() {
    @Autowired
    lateinit var evaluate: EvaluatePriceAlertsUseCase

    @Test
    fun `given one alert when ten deliveries of the same price event run at once then exactly one event is written`() {
        val (token, _) = aCustomer()
        val date = aFutureDate()
        val alert = body(createAlert(token, date, target = 300))["id"].asLong()
        val change = PriceChange(1, "DB1", "GRU", "GIG", date, BigDecimal("250.00"))
        val start = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(THREADS)

        val triggered =
            (1..THREADS).map {
                pool.submit<Int> {
                    start.await()
                    evaluate.execute(change)
                }
            }.also { start.countDown() }.sumOf { it.get() }
        pool.shutdown()

        assertEquals(1, triggered)
        assertEquals(1, outboxEvents("price-alert.triggered", alert.toString()).size)
    }

    private companion object {
        const val THREADS = 10
    }
}
