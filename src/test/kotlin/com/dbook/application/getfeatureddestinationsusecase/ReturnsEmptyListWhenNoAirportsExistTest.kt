package com.dbook.application.getfeatureddestinationsusecase

import kotlin.test.Test
import kotlin.test.assertTrue

class ReturnsEmptyListWhenNoAirportsExistTest : GetFeaturedDestinationsUseCaseFixture() {
    @Test
    fun `given no airports when executed then returns an empty list`() {
        val destinations = useCase(airports = emptyList()).execute()

        assertTrue(destinations.isEmpty())
    }
}
