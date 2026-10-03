package com.dbook.application.seating.getseatmapusecase

import com.dbook.domain.catalog.BookableNotFoundException
import kotlin.test.Test
import kotlin.test.assertFailsWith

class ThrowsWhenBookableDoesNotExistTest : GetSeatMapUseCaseFixture() {
    @Test
    fun `given a nonexistent bookableId when getting its seat map then it throws BookableNotFoundException`() {
        assertFailsWith<BookableNotFoundException> {
            useCase.execute(999)
        }
    }
}
