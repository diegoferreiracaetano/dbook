package com.dbook.presentation.promo

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import kotlin.test.Test
import kotlin.test.assertEquals

class TwoCustomersRacingForTheLastUseOnlyOneWinsTest : PromoApiFixture() {
    @Test
    fun `given one use left when two customers pay with it at once then one wins and the other stays PENDING`() {
        val (id, code) = newPromo(manager(), "maxRedemptions" to 1)
        val (tokenA, bookingA) = customerWithABooking()
        val (tokenB, bookingB) = customerWithABooking()
        val start = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(2)

        val statuses =
            listOf(tokenA to bookingA, tokenB to bookingB).map { (token, booking) ->
                pool.submit<Int> {
                    start.await()
                    payWith(token, listOf(booking), code).response.status
                }
            }.also { start.countDown() }.map { it.get() }
        pool.shutdown()

        assertEquals(listOf(201, 422), statuses.sorted())
        assertEquals(1, redeemed(id))
        assertEquals(listOf("CONFIRMED", "PENDING"), listOf(bookingA, bookingB).map(::statusOfBooking).sorted())
        assertEquals(
            1,
            jdbcTemplate.queryForObject(
                "SELECT count(*) FROM promo_redemption WHERE promo_id = ?",
                Int::class.java,
                id,
            ),
        )
    }
}
