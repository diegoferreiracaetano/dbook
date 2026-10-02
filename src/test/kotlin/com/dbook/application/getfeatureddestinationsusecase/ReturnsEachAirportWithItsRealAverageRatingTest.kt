package com.dbook.application.getfeatureddestinationsusecase

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ReturnsEachAirportWithItsRealAverageRatingTest : GetFeaturedDestinationsUseCaseFixture() {
    @Test
    fun `given airports with different ratings when executed then each keeps its own average`() {
        val destinations =
            useCase(
                airports = listOf(airport("GIG"), airport("JFK")),
                ratings = mapOf("GIG" to 4.5),
            ).execute()

        assertEquals(4.5, destinations[0].averageRating)
        assertNull(destinations[1].averageRating)
    }
}
