package com.dbook.application.favorite.addfavoriteusecase

import com.dbook.application.favorite.FavoriteFixture
import com.dbook.domain.catalog.AirportNotFoundException
import com.dbook.domain.catalog.BookableNotFoundException
import com.dbook.domain.favorite.FavoriteType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SavingIsIdempotentAndTheTargetMustExistTest : FavoriteFixture() {
    @Test
    fun `given a destination when saved twice then there is one favorite`() {
        add.execute(7, FavoriteType.DESTINATION, "GRU")
        add.execute(7, FavoriteType.DESTINATION, "GRU")

        assertEquals(1, favorites.all.size)
        assertEquals(now, favorites.all.single().createdAt)
    }

    @Test
    fun `given the same target saved by two customers when listed then each has their own`() {
        add.execute(7, FavoriteType.FLIGHT, "10")
        add.execute(8, FavoriteType.FLIGHT, "10")

        assertEquals(2, favorites.all.size)
    }

    @Test
    fun `given a destination or a flight that does not exist when saving then it is not found and nothing is stored`() {
        assertFailsWith<AirportNotFoundException> { add.execute(7, FavoriteType.DESTINATION, "XXX") }
        assertFailsWith<BookableNotFoundException> { add.execute(7, FavoriteType.FLIGHT, "999") }

        assertEquals(0, favorites.all.size)
    }

    @Test
    fun `given a malformed target when saving then it is refused`() {
        assertFailsWith<IllegalArgumentException> { add.execute(7, FavoriteType.DESTINATION, "gru") }
        assertFailsWith<IllegalArgumentException> { add.execute(7, FavoriteType.DESTINATION, "GRUU") }
        assertFailsWith<IllegalArgumentException> { add.execute(7, FavoriteType.FLIGHT, "abc") }
        assertFailsWith<IllegalArgumentException> { add.execute(7, FavoriteType.FLIGHT, "-1") }
    }
}
