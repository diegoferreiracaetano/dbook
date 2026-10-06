package com.dbook.domain.favorite

import java.time.Instant

enum class FavoriteType { DESTINATION, FLIGHT }

/**
 * Something a customer saved. [targetId] is the destination's IATA code or the flight's id, as text; the catalog is
 * what says whether it exists (the use case asks), the favorite only says whether it is well formed.
 */
data class Favorite(
    val userId: Long,
    val type: FavoriteType,
    val targetId: String,
    val createdAt: Instant,
) {
    init {
        when (type) {
            FavoriteType.DESTINATION ->
                require(targetId.length == IATA_LENGTH && targetId.all { it in 'A'..'Z' }) {
                    "a destination is identified by its $IATA_LENGTH-letter IATA code, in capitals"
                }
            FavoriteType.FLIGHT ->
                require(targetId.toLongOrNull()?.let { it > 0 } == true) { "a flight is identified by its numeric id" }
        }
    }

    private companion object {
        const val IATA_LENGTH = 3
    }
}

enum class FavoriteAddResult { ADDED, ALREADY_FAVORITE, LIMIT_REACHED }

class FavoritesLimitReachedException(val limit: Int) :
    RuntimeException("You can have at most $limit favorites; remove one first")

interface FavoriteRepository {
    /**
     * Adds the favorite unless the customer already has it (then it is a no-op, even at the limit) or already has
     * [limit] favorites. The decision is atomic: two requests at once can never take the customer past the limit.
     */
    fun add(
        favorite: Favorite,
        limit: Int,
    ): FavoriteAddResult

    /** @return whether there was one to remove; removing what is not there is fine. */
    fun remove(
        userId: Long,
        type: FavoriteType,
        targetId: String,
    ): Boolean
}
