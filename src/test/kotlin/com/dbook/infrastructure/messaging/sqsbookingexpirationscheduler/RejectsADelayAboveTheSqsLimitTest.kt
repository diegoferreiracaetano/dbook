package com.dbook.infrastructure.messaging.sqsbookingexpirationscheduler

import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertFailsWith

class RejectsADelayAboveTheSqsLimitTest : SqsBookingExpirationSchedulerFixture() {
    @Test
    fun `given a delay above 15 minutes when scheduling then it is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            schedulerFor(createQueue()).scheduleExpiration(bookingId = 1L, after = Duration.ofMinutes(16))
        }
    }
}
