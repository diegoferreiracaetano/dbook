package com.dbook.application.review.getaverageratingfordestinationusecase

import com.dbook.application.review.GetAverageRatingForDestinationUseCase
import com.dbook.application.review.createreviewusecase.FakeReviewRepository
import kotlin.test.Test
import kotlin.test.assertEquals

class ReturnsTheAverageOfTheRequestedDestinationTest {
    @Test
    fun `given reviews for two destinations when executed then returns the average of the requested one`() {
        val useCase =
            GetAverageRatingForDestinationUseCase(
                FakeReviewRepository(averageRatingByDestination = mapOf("GIG" to 4.5, "JFK" to 3.0)),
            )

        assertEquals(4.5, useCase.execute("GIG"))
    }
}
