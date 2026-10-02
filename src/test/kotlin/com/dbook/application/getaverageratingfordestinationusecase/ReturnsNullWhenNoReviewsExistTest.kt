package com.dbook.application.getaverageratingfordestinationusecase

import com.dbook.application.GetAverageRatingForDestinationUseCase
import com.dbook.application.createreviewusecase.FakeReviewRepository
import kotlin.test.Test
import kotlin.test.assertNull

class ReturnsNullWhenNoReviewsExistTest {
    @Test
    fun `given reviews only for other destinations when executed then returns null`() {
        val useCase =
            GetAverageRatingForDestinationUseCase(
                FakeReviewRepository(averageRatingByDestination = mapOf("GIG" to 4.5)),
            )

        assertNull(useCase.execute("JFK"))
    }
}
