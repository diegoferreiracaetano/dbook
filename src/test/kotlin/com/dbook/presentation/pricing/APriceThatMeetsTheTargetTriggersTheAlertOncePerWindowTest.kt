package com.dbook.presentation.pricing

import com.dbook.application.pricing.EvaluatePriceAlertsUseCase
import com.dbook.domain.pricing.PriceChange
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class APriceThatMeetsTheTargetTriggersTheAlertOncePerWindowTest : PricingApiFixture() {
    @Autowired
    lateinit var evaluate: EvaluatePriceAlertsUseCase

    private fun priced(
        date: LocalDate,
        price: String,
    ) = PriceChange(1, "DB1", "GRU", "GIG", date, BigDecimal(price))

    private fun triggeredFor(alertId: Long) = outboxEvents("price-alert.triggered", alertId.toString())

    @Test
    fun `given a target of 300 when the price falls to 250 then the customer is told once, not twice`() {
        val (token, customer) = aCustomer()
        val date = aFutureDate()
        val alert = body(createAlert(token, date, target = 300))["id"].asLong()

        assertEquals(1, evaluate.execute(priced(date, "250.00")))
        assertEquals(0, evaluate.execute(priced(date, "250.00")))

        val events = triggeredFor(alert)
        assertEquals(1, events.size)
        assertEquals(customer.toLong(), events[0]["customerId"].asLong())
        assertEquals("250.00", events[0]["price"].asText())
        assertEquals("300.00", events[0]["targetPrice"].asText())
    }

    @Test
    fun `given a higher price, another date or route, or an alert switched off when evaluated then nothing triggers`() {
        val (token, _) = aCustomer()
        val date = aFutureDate()
        val alert = body(createAlert(token, date, target = 300))["id"].asLong()

        assertEquals(0, evaluate.execute(priced(date, "300.01")))
        assertEquals(0, evaluate.execute(priced(date.plusDays(1), "100.00")))
        assertEquals(0, evaluate.execute(PriceChange(1, "DB1", "GIG", "GRU", date, BigDecimal("100.00"))))
        updateAlert(token, alert, mapOf("active" to false))
        assertEquals(0, evaluate.execute(priced(date, "100.00")))

        assertEquals(0, triggeredFor(alert).size)
    }

    @Test
    fun `given a price exactly at the target when evaluated then it triggers, the target is included`() {
        val (token, _) = aCustomer()
        val date = aFutureDate()
        createAlert(token, date, target = 300)

        assertEquals(1, evaluate.execute(priced(date, "300.00")))
    }

    @Test
    fun `given a price that bounces inside the window when evaluated then it is told once, and again after it`() {
        val (token, _) = aCustomer()
        val date = aFutureDate()
        val alert = body(createAlert(token, date, target = 300))["id"].asLong()

        evaluate.execute(priced(date, "250.00"))
        evaluate.execute(priced(date, "400.00"))
        evaluate.execute(priced(date, "260.00"))
        assertEquals(1, triggeredFor(alert).size)

        jdbcTemplate.update("UPDATE price_alert SET last_notified_at = now() - interval '25 hours' WHERE id = ?", alert)
        evaluate.execute(priced(date, "240.00"))

        assertEquals(2, triggeredFor(alert).size)
    }

    @Test
    fun `given a changed target when the price that already met the old one comes again then it is told again`() {
        val (token, _) = aCustomer()
        val date = aFutureDate()
        val alert = body(createAlert(token, date, target = 300))["id"].asLong()
        evaluate.execute(priced(date, "250.00"))

        updateAlert(token, alert, mapOf("targetPrice" to 260))
        evaluate.execute(priced(date, "255.00"))

        assertEquals(2, triggeredFor(alert).size)
    }
}
