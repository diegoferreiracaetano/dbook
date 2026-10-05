package com.dbook.presentation.promo

import kotlin.test.Test
import kotlin.test.assertEquals

class PayingWithACodeChargesLessAndEveryoneAgreesOnTheNumbersTest : PromoApiFixture() {
    @Test
    fun `given a preview then a payment with the code then the preview used nothing and the payment charges 90`() {
        val team = manager()
        val (id, code) = newPromo(team)
        val (token, booking) = customerWithABooking()

        val shown = json(preview(token, code.lowercase(), listOf(booking)))
        assertEquals(100.0, shown["subtotal"].asDouble())
        assertEquals(10.0, shown["discount"].asDouble())
        assertEquals(90.0, shown["total"].asDouble())
        assertEquals(0, redeemed(id))

        val paid = payWith(token, listOf(booking), code.lowercase())

        assertEquals(201, paid.response.status)
        assertEquals(90.0, json(paid)["amount"].asDouble())
        assertEquals(100.0, json(paid)["subtotal"].asDouble())
        assertEquals(10.0, json(paid)["discount"].asDouble())
        assertEquals(code, json(paid)["promoCode"].asText())
        assertEquals(1, redeemed(id))
    }

    @Test
    fun `given a paid booking when the customer lists trips then the price is frozen and the discount shows`() {
        val (_, code) = newPromo(manager())
        val (token, booking) = customerWithABooking()
        payWith(token, listOf(booking), code)

        val mine = json(myBookings(token)).first { it["id"].asLong() == booking }

        assertEquals(100.0, mine["price"].asDouble())
        assertEquals(10.0, mine["discount"].asDouble())
        assertEquals(90.0, mine["paidAmount"].asDouble())
    }

    @Test
    fun `given a used code when the team lists its redemptions then who, which payment and how much are there`() {
        val team = manager()
        val (id, code) = newPromo(team)
        val (token, booking) = customerWithABooking()
        val paymentId = json(payWith(token, listOf(booking), code))["id"].asLong()

        val rows = json(adminGet(team, "/v1/admin/promo-codes/$id/redemptions"))["items"]
        val listed = json(adminGet(team, "/v1/admin/promo-codes/$id"))

        assertEquals(1, rows.size())
        assertEquals(paymentId, rows[0]["paymentId"].asLong())
        assertEquals(10.0, rows[0]["discount"].asDouble())
        assertEquals(1, listed["redeemed"].asInt())
    }

    @Test
    fun `given a fixed code when paying two legs then the discount is split between them`() {
        val (_, code) = newPromo(manager(), "type" to "FIXED", "value" to 30)
        val token = registerAndLogin(uniqueEmail())
        val legs = bookSeats(token, 2)

        val paid = payWith(token, legs, code)

        assertEquals(170.0, json(paid)["amount"].asDouble())
        val mine = json(myBookings(token))
        assertEquals(
            listOf(15.0, 15.0),
            legs.map {
                    id ->
                mine.first { it["id"].asLong() == id }["discount"].asDouble()
            },
        )
    }
}
