package com.dbook.infrastructure.messaging.expiration.sqsbookingexpirationscheduler

import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SendsTheBookingIdWithTheRequestedDelayTest : SqsBookingExpirationSchedulerFixture() {
    @Test
    fun `given a delay when scheduling then the message stays hidden until it elapses and carries the booking id`() {
        val queueUrl = createQueue()

        schedulerFor(queueUrl).scheduleExpiration(bookingId = 42L, after = Duration.ofSeconds(2))

        assertTrue(receive(queueUrl, waitSeconds = 0).isEmpty(), "must not be visible before the delay")
        assertEquals(listOf("""{"bookingId":42}"""), receive(queueUrl, waitSeconds = 5).map { it.body() })
    }
}
