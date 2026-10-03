package com.dbook.application.payment.registerpaymentusecase

import com.dbook.domain.booking.NotBookingOwnerException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DoesNotReplayAnotherUsersPaymentTest : RegisterPaymentUseCaseFixture() {
    @Test
    fun `given a key used by one user when another user sends the same key then it is treated as a new request`() {
        executeCommitted(command())

        // not answered with the owner's payment: for this user the key is unknown, so the
        // request proceeds as a new one and is rejected for paying a booking that isn't theirs
        assertFailsWith<NotBookingOwnerException> {
            executeCommitted(command(requestingUserId = 999L))
        }
        assertEquals(1, paymentRepository.saved.size)
    }
}
