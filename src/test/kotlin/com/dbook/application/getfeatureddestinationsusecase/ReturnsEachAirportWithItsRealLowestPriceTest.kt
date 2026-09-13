package com.dbook.application.getfeatureddestinationsusecase

import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals

class ReturnsEachAirportWithItsRealLowestPriceTest : GetFeaturedDestinationsUseCaseFixture() {
    @Test
    fun `given airports with different prices when executed then each keeps its own lowest price`() {
        val destinations =
            useCase(
                airports = listOf(airport("GIG"), airport("JFK")),
                prices = mapOf("GIG" to BigDecimal("305.00"), "JFK" to BigDecimal("304.00")),
            ).execute()

        assertEquals(BigDecimal("305.00"), destinations[0].lowestPrice)
        assertEquals(BigDecimal("304.00"), destinations[1].lowestPrice)
    }
}
