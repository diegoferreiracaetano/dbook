package com.dbook.application.getfeatureddestinationsusecase

import kotlin.test.Test
import kotlin.test.assertNull

class ReturnsNullPriceWhenAnAirportHasNoFlightsTest : GetFeaturedDestinationsUseCaseFixture() {
    @Test
    fun `given an airport with no flights when executed then its lowest price is null`() {
        val destinations = useCase(airports = listOf(airport("XXX"))).execute()

        assertNull(destinations.single().lowestPrice)
    }
}
