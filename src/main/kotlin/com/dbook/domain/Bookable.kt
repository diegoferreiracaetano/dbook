package com.dbook.domain

import java.math.BigDecimal

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
