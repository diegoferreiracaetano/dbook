package com.dbook.domain.booking

import java.time.Duration

/**
 * Schedules the automatic expiration of a PENDING [Booking] (see the
 * `infrastructure.messaging` package for the SQS implementation). Always called only after
 * the booking's transaction commits.
 */
interface BookingExpirationScheduler {
    fun scheduleExpiration(
        bookingId: Long,
        after: Duration,
    )
}
