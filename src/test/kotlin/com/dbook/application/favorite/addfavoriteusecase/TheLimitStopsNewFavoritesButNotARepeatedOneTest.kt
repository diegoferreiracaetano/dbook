package com.dbook.application.favorite.addfavoriteusecase

import com.dbook.application.favorite.FavoriteFixture
import com.dbook.application.favorite.MAX_FAVORITES_PER_USER
import com.dbook.domain.favorite.Favorite
import com.dbook.domain.favorite.FavoriteType
import com.dbook.domain.favorite.FavoritesLimitReachedException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TheLimitStopsNewFavoritesButNotARepeatedOneTest : FavoriteFixture() {
    private fun fillTheLimit() {
        repeat(MAX_FAVORITES_PER_USER - 1) {
            favorites.all += Favorite(7, FavoriteType.FLIGHT, "${1000 + it}", now)
        }
        add.execute(7, FavoriteType.DESTINATION, "GRU")
    }

    @Test
    fun `given the limit reached when saving a new one then it is refused, and another customer is not affected`() {
        fillTheLimit()

        assertFailsWith<FavoritesLimitReachedException> { add.execute(7, FavoriteType.DESTINATION, "GIG") }
        add.execute(8, FavoriteType.DESTINATION, "GIG")

        assertEquals(MAX_FAVORITES_PER_USER, favorites.all.count { it.userId == 7L })
    }

    @Test
    fun `given the limit reached when saving one already saved then it is still a success`() {
        fillTheLimit()

        add.execute(7, FavoriteType.DESTINATION, "GRU")

        assertEquals(MAX_FAVORITES_PER_USER, favorites.all.count { it.userId == 7L })
    }

    @Test
    fun `given the limit reached when one is removed then a new one fits`() {
        fillTheLimit()
        remove.execute(7, FavoriteType.DESTINATION, "GRU")

        add.execute(7, FavoriteType.DESTINATION, "GIG")

        assertEquals(MAX_FAVORITES_PER_USER, favorites.all.count { it.userId == 7L })
    }
}
