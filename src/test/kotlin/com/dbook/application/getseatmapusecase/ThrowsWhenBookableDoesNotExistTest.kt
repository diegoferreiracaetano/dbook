package com.dbook.application.getseatmapusecase

import com.dbook.domain.BookableNotFoundException
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
