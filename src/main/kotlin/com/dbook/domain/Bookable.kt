package com.dbook.domain

import java.math.BigDecimal

/**
 * Anything that can be reserved through a [Booking] — [Flight] today, `Accommodation`
 * (hotels) planned as a second specialization. Carries availability tracking and
 * pricing common to every specialization; route/schedule/seat details live on the
 * concrete subtype instead.
 *
 * No abstract member on purpose: exists only to be extended, never instantiated on its
 * own — `abstract` enforces that at compile time.
 */
@Suppress("UnnecessaryAbstractClass")
abstract class Bookable(
    open val id: Long? = null,
    open val title: String,
    open val price: BigDecimal,
    open val totalCapacity: Int,
    open val availableCapacity: Int,
    open val active: Boolean = true,
) {
    init {
        require(price >= BigDecimal.ZERO) { "price must not be negative" }
        require(totalCapacity >= 0) { "totalCapacity must not be negative" }
        require(availableCapacity in 0..totalCapacity) {
            "availableCapacity must be between 0 and totalCapacity"
        }
    }
}
