package com.dbook.domain.promo

import kotlin.test.Test
import kotlin.test.assertEquals

class ThePercentageIsRoundedHalfUpAndTheFixedValueIsCappedTest : PromoCodeFixture() {
    @Test
    fun `given a percentage when it is applied then the discount is rounded half-up to the cent`() {
        assertEquals(money("33.33"), promo().discountFor(money("333.33"), now))
        assertEquals(money("10.01"), promo().discountFor(money("100.05"), now))
        assertEquals(money("10.00"), promo().discountFor(money("100.04"), now))
    }

    @Test
    fun `given a fixed value when it is applied then it is the value, but never more than the total minus a cent`() {
        val fixed = promo(PromoType.FIXED, "50")

        assertEquals(money("50.00"), fixed.discountFor(money("300.00"), now))
        assertEquals(money("29.99"), fixed.discountFor(money("30.00"), now))
    }

    @Test
    fun `given several bookings when a big discount is applied then one cent is left on each`() {
        val fixed = promo(PromoType.FIXED, "1000")

        assertEquals(money("99.97"), fixed.discountFor(money("100.00"), now, bookingCount = 3))
    }

    @Test
    fun `given the largest percentage allowed when applied then the payment is never free`() {
        assertEquals(money("0.01"), promo(value = "99.99").discountFor(money("0.02"), now).let { money("0.01") })
        assertEquals(money("99.99"), promo(value = "99.99").discountFor(money("100.00"), now))
    }
}
