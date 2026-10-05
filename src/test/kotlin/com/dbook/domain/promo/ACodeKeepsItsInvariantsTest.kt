package com.dbook.domain.promo

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ACodeKeepsItsInvariantsTest : PromoCodeFixture() {
    @Test
    fun `given bad data when a code is created then it is refused`() {
        assertFailsWith<IllegalArgumentException> { promo { copy(code = "ab") } }
        assertFailsWith<IllegalArgumentException> { promo { copy(code = "has space") } }
        assertFailsWith<IllegalArgumentException> { promo { copy(code = "lowercase") } }
        assertFailsWith<IllegalArgumentException> { promo(value = "0") }
        assertFailsWith<IllegalArgumentException> { promo(value = "100") }
        assertFailsWith<IllegalArgumentException> { promo { copy(validUntil = from) } }
        assertFailsWith<IllegalArgumentException> { promo { copy(minAmount = money("-1")) } }
        assertFailsWith<IllegalArgumentException> { promo { copy(maxRedemptions = 0) } }
        assertFailsWith<IllegalArgumentException> { promo { copy(maxPerUser = 0) } }
    }

    @Test
    fun `given a limit below what was already used when changed then it is refused`() {
        assertFailsWith<IllegalArgumentException> { promo { copy(maxRedemptions = 3, redeemed = 4) } }
    }

    @Test
    fun `given a fixed value of 100 or more when created then it is fine, only a percentage must stay below 100`() {
        assertEquals(money("150"), promo(PromoType.FIXED, "150").value)
    }

    @Test
    fun `given a code typed in any way when normalized then it is trimmed and in capitals`() {
        assertEquals("WELCOME10", PromoCode.normalize("  welcome10 "))
    }

    @Test
    fun `given a code with a limit when it is used up then it is exhausted`() {
        assertEquals(true, promo { copy(maxRedemptions = 2, redeemed = 2) }.isExhausted)
        assertEquals(false, promo { copy(maxRedemptions = 2, redeemed = 1) }.isExhausted)
        assertEquals(false, promo { copy(redeemed = 1_000) }.isExhausted)
    }
}
