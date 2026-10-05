package com.dbook.domain.promo

import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ACodeOnlyWorksInsideItsWindowAndAboveItsMinimumTest : PromoCodeFixture() {
    private fun rejectionOf(
        promo: PromoCode,
        total: String = "100.00",
        at: java.time.Instant = now,
    ): PromoRejection = assertFailsWith<PromoRejectedException> { promo.discountFor(money(total), at) }.reason

    @Test
    fun `given the edges of the window when applied then the start is included and the end is not`() {
        assertEquals(money("10.00"), promo().discountFor(money("100.00"), from))
        assertEquals(PromoRejection.NOT_STARTED, rejectionOf(promo(), at = from.minusMillis(1)))
        assertEquals(PromoRejection.EXPIRED, rejectionOf(promo(), at = until))
        assertEquals(money("10.00"), promo().discountFor(money("100.00"), until.minus(Duration.ofMillis(1))))
    }

    @Test
    fun `given an inactive code when applied then it is rejected as inactive`() {
        assertEquals(PromoRejection.INACTIVE, rejectionOf(promo { copy(active = false) }))
    }

    @Test
    fun `given a minimum when the total is below it then it is rejected, and at it then it is accepted`() {
        val withMinimum = promo { copy(minAmount = money("200.00")) }

        assertEquals(PromoRejection.BELOW_MINIMUM, rejectionOf(withMinimum, total = "199.99"))
        assertEquals(money("20.00"), withMinimum.discountFor(money("200.00"), now))
    }
}
