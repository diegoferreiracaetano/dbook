package com.dbook.presentation.destinationcontroller

import com.dbook.application.FeaturedDestination
import com.dbook.domain.Airport
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.test.web.servlet.get
import java.math.BigDecimal

class ReturnsTheFeaturedDestinationsListTest : DestinationControllerFixture() {
    @Test
    fun `given known destinations when listed then it returns 200 with each destination and its price`() {
        val gig =
            Airport(
                id = 1,
                iataCode = "GIG",
                name = "Galeão",
                city = "Rio de Janeiro",
                country = "Brasil",
                photoUrl = "https://example.com/gig.jpg",
            )
        given(getFeaturedDestinationsUseCase.execute())
            .willReturn(listOf(FeaturedDestination(gig, BigDecimal("305.00"))))

        mockMvc.get("/destinations").andExpect {
            status { isOk() }
            jsonPath("$[0].iataCode") { value("GIG") }
            jsonPath("$[0].city") { value("Rio de Janeiro") }
            jsonPath("$[0].photoUrl") { value("https://example.com/gig.jpg") }
            jsonPath("$[0].lowestPrice") { value(305.00) }
        }
    }
}
