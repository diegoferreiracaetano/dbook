package com.dbook

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/** A clock the test moves by hand, so that leases and backoffs can be crossed without waiting. */
class MutableClock(private var now: Instant = Instant.now()) : Clock() {
    fun advance(by: Duration) {
        now = now.plus(by)
    }

    override fun instant(): Instant = now

    override fun getZone(): ZoneId = ZoneOffset.UTC

    override fun withZone(zone: ZoneId): Clock = this
}
