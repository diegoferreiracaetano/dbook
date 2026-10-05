package com.dbook.application.favorite

import com.dbook.domain.catalog.AirportNotFoundException
import com.dbook.domain.catalog.AirportRepository
import com.dbook.domain.catalog.BookableNotFoundException
import com.dbook.domain.catalog.FlightRepository
import com.dbook.domain.common.PageQuery
import com.dbook.domain.common.PageResult
import com.dbook.domain.favorite.Favorite
import com.dbook.domain.favorite.FavoriteAddResult
import com.dbook.domain.favorite.FavoriteReader
import com.dbook.domain.favorite.FavoriteRepository
import com.dbook.domain.favorite.FavoriteType
import com.dbook.domain.favorite.FavoriteView
import com.dbook.domain.favorite.FavoritesLimitReachedException
import io.micrometer.observation.annotation.Observed
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

/** The most favorites a customer can keep. */
const val MAX_FAVORITES_PER_USER = 200

/**
 * Saves a destination or a flight. Idempotent: saving what is already saved is a success that changes nothing, so a
 * client that did not see the answer can simply send it again. The target has to exist in the catalog.
 */
@Observed(name = "dbook.usecase")
@Service
class AddFavoriteUseCase(
    private val favoriteRepository: FavoriteRepository,
    private val airportRepository: AirportRepository,
    private val flightRepository: FlightRepository,
    private val clock: Clock,
) {
    @Transactional
    fun execute(
        userId: Long,
        type: FavoriteType,
        targetId: String,
    ) {
        val favorite = Favorite(userId, type, targetId, clock.instant())
        requireTargetExists(favorite)
        if (favoriteRepository.add(favorite, MAX_FAVORITES_PER_USER) == FavoriteAddResult.LIMIT_REACHED) {
            throw FavoritesLimitReachedException(MAX_FAVORITES_PER_USER)
        }
    }

    private fun requireTargetExists(favorite: Favorite) {
        when (favorite.type) {
            FavoriteType.DESTINATION ->
                airportRepository.findByIataCode(favorite.targetId) ?: throw AirportNotFoundException(favorite.targetId)
            FavoriteType.FLIGHT ->
                flightRepository.findById(favorite.targetId.toLong())
                    ?: throw BookableNotFoundException(favorite.targetId.toLong())
        }
    }
}

/** Removes a favorite. Idempotent too: removing what is not there is a success. */
@Observed(name = "dbook.usecase")
@Service
class RemoveFavoriteUseCase(
    private val favoriteRepository: FavoriteRepository,
) {
    @Transactional
    fun execute(
        userId: Long,
        type: FavoriteType,
        targetId: String,
    ) {
        favoriteRepository.remove(userId, type, targetId)
    }
}

@Observed(name = "dbook.usecase")
@Service
class ListFavoritesUseCase(
    private val favoriteReader: FavoriteReader,
) {
    fun execute(
        userId: Long,
        type: FavoriteType?,
        page: PageQuery,
    ): PageResult<FavoriteView> = favoriteReader.list(userId, type, page)
}
