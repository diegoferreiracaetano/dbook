package com.dbook.domain.promo

import java.math.BigDecimal
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TheDiscountIsSplitToTheCentWithoutLeavingABookingAtZeroTest {
    private fun prices(vararg values: String) = values.map(::BigDecimal)

    @Test
    fun `given two prices when a discount is split then each share is proportional`() {
        val shares = allocateDiscount(BigDecimal("40.00"), prices("500.00", "300.00"))

        assertEquals(listOf(BigDecimal("25.00"), BigDecimal("15.00")), shares)
    }

    @Test
    fun `given an uneven discount when split then the lost cents are given out and the sum is exact`() {
        val shares = allocateDiscount(BigDecimal("10.00"), prices("100.00", "100.00", "100.00"))

        assertEquals(BigDecimal("10.00"), shares.fold(BigDecimal.ZERO, BigDecimal::add))
        assertTrue(shares.all { it >= BigDecimal("3.33") && it <= BigDecimal("3.34") })
    }

    @Test
    fun `given the largest discount allowed when split then every booking still has a cent to pay`() {
        val prices = prices("0.01", "299.99")
        val shares = allocateDiscount(maxDiscount(BigDecimal("300.00"), 2), prices)

        assertTrue(prices.zip(shares).all { (price, share) -> price - share >= BigDecimal("0.01") })
        assertEquals(maxDiscount(BigDecimal("300.00"), 2), shares.fold(BigDecimal.ZERO, BigDecimal::add))
    }

    @Test
    fun `given any prices and any allowed discount when split then the shares add up exactly and leave a cent each`() {
        val random = Random(42)
        repeat(RUNS) {
            val count = random.nextInt(1, 5)
            val prices = List(count) { BigDecimal(random.nextInt(1, 100_000)).movePointLeft(2) }
            val total = prices.fold(BigDecimal.ZERO, BigDecimal::add)
            val discount =
                BigDecimal(
                    random.nextLong(0, maxDiscount(total, count).movePointRight(2).toLong() + 1),
                ).movePointLeft(2)

            val shares = allocateDiscount(discount, prices)

            assertEquals(discount, shares.fold(BigDecimal.ZERO, BigDecimal::add), "prices=$prices discount=$discount")
            assertTrue(
                prices.zip(shares).all {
                        (price, share) ->
                    share >= BigDecimal.ZERO && price - share >= BigDecimal("0.01")
                },
            )
        }
    }

    @Test
    fun `given a discount above the allowed maximum when split then it is refused`() {
        assertFailsWith<IllegalArgumentException> { allocateDiscount(BigDecimal("300.00"), prices("300.00")) }
    }

    private companion object {
        const val RUNS = 500
    }
}
