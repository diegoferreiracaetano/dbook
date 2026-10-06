package com.dbook.domain.favorite

import com.dbook.domain.common.PageQuery
import com.dbook.domain.common.PageResult
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime

data class FavoriteDestination(
    val iataCode: String,
    val name: String,
    val city: String,
    val country: String,
    val photoUrl: String,
)

data class FavoriteFlight(
    val id: Long,
    val flightNumber: String,
    val origin: String,
    val destination: String,
    val departureTime: LocalDateTime,
    val price: BigDecimal,
    val onSale: Boolean,
)

/** A favorite with what the app needs to draw it: exactly one of [destination] and [flight] is set. */
data class FavoriteView(
    val type: FavoriteType,
    val targetId: String,
    val createdAt: Instant,
    val destination: FavoriteDestination?,
    val flight: FavoriteFlight?,
)

interface FavoriteReader {
    /** Newest first; [type] narrows to one kind. */
    fun list(
        userId: Long,
        type: FavoriteType?,
        page: PageQuery,
    ): PageResult<FavoriteView>
}
