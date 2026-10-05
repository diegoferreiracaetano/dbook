package com.dbook.presentation.pricing

import org.springframework.test.web.servlet.get
import kotlin.test.Test
import kotlin.test.assertEquals

class PriceAlertsAreManagedByTheirOwnerWithinTheirLimitsTest : PricingApiFixture() {
    @Test
    fun `given a route and a date when an alert is created then it is listed, changed and deleted by its owner`() {
        val (token, _) = aCustomer()
        val date = aFutureDate()

        val created = createAlert(token, date, target = 300)
        assertEquals(201, created.response.status)
        val id = body(created)["id"].asLong()
        assertEquals(true, body(created)["active"].asBoolean())
        assertEquals(listOf(id), body(myAlerts(token))["items"].map { it["id"].asLong() })

        val changed = updateAlert(token, id, mapOf("targetPrice" to 250, "active" to false))
        assertEquals(250.0, body(changed)["targetPrice"].asDouble())
        assertEquals(false, body(changed)["active"].asBoolean())

        assertEquals(204, deleteAlert(token, id).response.status)
        assertEquals(0, body(myAlerts(token))["items"].size())
    }

    @Test
    fun `given an alert when someone else changes or deletes it then it is a 404, and their list does not show it`() {
        val (owner, _) = aCustomer()
        val (other, _) = aCustomer()
        val id = body(createAlert(owner, aFutureDate()))["id"].asLong()

        assertEquals(404, updateAlert(other, id, mapOf("active" to false)).response.status)
        assertEquals(404, deleteAlert(other, id).response.status)
        assertEquals(0, body(myAlerts(other))["items"].size())
        assertEquals(true, body(myAlerts(owner))["items"][0]["active"].asBoolean())
    }

    @Test
    fun `given a second alert on the same route and date when created then it is a 409`() {
        val (token, _) = aCustomer()
        val date = aFutureDate()
        createAlert(token, date)

        assertEquals(409, createAlert(token, date, target = 200).response.status)
    }

    @Test
    fun `given bad input when an alert is created then it is a 400, and an unknown airport a 404`() {
        val (token, _) = aCustomer()

        assertEquals(400, createAlert(token, java.time.LocalDate.now().minusDays(1)).response.status)
        assertEquals(400, createAlert(token, aFutureDate(), target = 0).response.status)
        assertEquals(400, createAlert(token, aFutureDate(), destination = "GRU").response.status)
        assertEquals(400, createAlert(token, aFutureDate(), origin = "gru").response.status)
        assertEquals(404, createAlert(token, aFutureDate(), destination = "QQQ").response.status)
        assertEquals(400, updateAlert(token, 1, emptyMap()).response.status)
    }

    @Test
    fun `given 20 active alerts when creating the 21st then it is a 409 PRICE_ALERTS_LIMIT until one is off`() {
        val (token, userId) = aCustomer()
        val base = aFutureDate()
        jdbcTemplate.update(
            "INSERT INTO price_alert (user_id, origin, destination, travel_date, target_price, created_at) " +
                "SELECT ?, 'GRU', 'GIG', ?::date + n, 100, now() FROM generate_series(1, 20) AS n",
            userId,
            java.sql.Date.valueOf(base),
        )

        val refused = createAlert(token, base.plusDays(100))

        assertEquals(409, refused.response.status)
        assertEquals("PRICE_ALERTS_LIMIT", body(refused)["code"].asText())
        jdbcTemplate.update(
            "UPDATE price_alert SET active = false WHERE id = (SELECT min(id) FROM price_alert WHERE user_id = ?)",
            userId,
        )
        assertEquals(201, createAlert(token, base.plusDays(100)).response.status)
    }

    @Test
    fun `given no token when using the alerts then it is a 401`() {
        assertEquals(401, mockMvc.get("/v1/price-alerts").andReturn().response.status)
    }
}
