package com.dbook.application.booking.expirebookingusecase

import kotlin.test.Test
import kotlin.test.assertEquals

class CountsAnUnknownBookingAsIgnoredTest : ExpireBookingUseCaseFixture() {
    @Test
    fun `given an unknown booking id when its expiration arrives then it is counted as ignored`() {
        withTransactionSynchronization { expireBookingUseCase.execute(999L) }

        assertEquals(1.0, counted("dbook.booking.expiration", "ignored"))
        assertEquals(0.0, counted("dbook.booking.expiration", "expired"))
    }
}
