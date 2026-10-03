package com.dbook.infrastructure.messaging.sqsbookingexpirationscheduler

import java.net.URI
import java.time.Duration
import kotlin.test.Test

class DoesNotThrowWhenTheQueueIsUnreachableTest : SqsBookingExpirationSchedulerFixture() {
    @Test
    fun `given an unreachable queue when scheduling then it logs and does not throw`() {
        val unreachable = clientFor(URI.create("http://localhost:1"))

        // reaching the end of the call without an exception IS the assertion: this runs
        // after the booking committed, so it must never turn into an error for the caller
        schedulerFor("http://localhost:1/000000000000/none", unreachable)
            .scheduleExpiration(bookingId = 1L, after = Duration.ofMinutes(15))
    }
}
