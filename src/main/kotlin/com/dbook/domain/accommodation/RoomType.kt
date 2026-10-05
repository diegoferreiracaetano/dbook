package com.dbook.domain.accommodation

import java.math.BigDecimal

private const val MAX_NAME_LENGTH = 100

/** A kind of room: how many guests it takes, its nightly rate and how many of them the hotel has. */
data class RoomType(
    val id: Long? = null,
    val name: String,
    val capacity: Int,
    val nightlyRate: BigDecimal,
    val quantity: Int,
    val active: Boolean = true,
) {
    init {
        require(
            name.isNotBlank() && name.length <= MAX_NAME_LENGTH,
        ) { "name must have 1 to $MAX_NAME_LENGTH characters" }
        require(capacity >= 1) { "capacity must be at least 1 guest" }
        require(nightlyRate > BigDecimal.ZERO) { "nightlyRate must be positive" }
        require(quantity >= 1) { "quantity must be at least 1 room" }
    }
}
