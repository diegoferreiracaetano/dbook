package com.dbook.application.getaverageratingfordestinationusecase

import com.dbook.application.GetAverageRatingForDestinationUseCase
import com.dbook.application.createreviewusecase.FakeReviewRepository
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
