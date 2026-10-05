package com.dbook.domain.promo

import java.math.BigDecimal
import java.time.Instant

// The clock is 2026-10-04 12:00; a code is valid from 2026-10-01 to 2026-10-31.
abstract class PromoCodeFixture {
    protected val now: Instant = Instant.parse("2026-10-04T12:00:00Z")
    protected val from: Instant = Instant.parse("2026-10-01T00:00:00Z")
    protected val until: Instant = Instant.parse("2026-10-31T00:00:00Z")

    protected fun money(value: String) = BigDecimal(value)

    protected fun promo(
        type: PromoType = PromoType.PERCENT,
        value: String = "10",
        change: PromoCode.() -> PromoCode = { this },
    ) = PromoCode(
        code = "WELCOME10",
        type = type,
        value = BigDecimal(value),
        validFrom = from,
        validUntil = until,
        createdBy = 1,
        createdAt = from,
    ).change()
}
