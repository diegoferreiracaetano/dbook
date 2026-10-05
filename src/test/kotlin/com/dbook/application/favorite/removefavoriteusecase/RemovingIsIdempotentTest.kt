package com.dbook.application.favorite.removefavoriteusecase

import com.dbook.application.favorite.FavoriteFixture
import com.dbook.domain.favorite.FavoriteType
import kotlin.test.Test
import kotlin.test.assertEquals

class RemovingIsIdempotentTest : FavoriteFixture() {
    @Test
    fun `given a favorite when removed twice then it is gone and the second time is a success`() {
        add.execute(7, FavoriteType.DESTINATION, "GRU")

        remove.execute(7, FavoriteType.DESTINATION, "GRU")
        remove.execute(7, FavoriteType.DESTINATION, "GRU")

        assertEquals(0, favorites.all.size)
    }

    @Test
    fun `given someone else's favorite when removing then it stays`() {
        add.execute(8, FavoriteType.DESTINATION, "GRU")

        remove.execute(7, FavoriteType.DESTINATION, "GRU")

        assertEquals(1, favorites.all.size)
    }
}
