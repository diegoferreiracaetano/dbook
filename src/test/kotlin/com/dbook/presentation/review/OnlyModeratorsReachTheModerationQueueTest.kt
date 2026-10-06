package com.dbook.presentation.review

import com.dbook.domain.common.access.Role
import org.springframework.test.web.servlet.get
import kotlin.test.Test
import kotlin.test.assertEquals

class OnlyModeratorsReachTheModerationQueueTest : ReviewFixture() {
    @Test
    fun `given staff without the permission, a customer and nobody when listing then 403, 403, 401, and support 200`() {
        val catalogManager = registerStaffAndLogin(uniqueEmail(), Role.CATALOG_MANAGER)
        val (customer) = reviewer(newDestination(), 4)

        assertEquals(403, adminReviews(catalogManager, "REPORTED").response.status)
        assertEquals(403, adminReviews(customer, "REPORTED").response.status)
        assertEquals(401, mockMvc.get("/v1/admin/reviews").andReturn().response.status)
        assertEquals(200, adminReviews(support(), "REPORTED").response.status)
    }

    @Test
    fun `given staff without the permission when hiding a review then it is a 403 and the review stays`() {
        val destination = newDestination()
        val author = reviewer(destination, 4)
        val catalogManager = registerStaffAndLogin(uniqueEmail(), Role.CATALOG_MANAGER)

        assertEquals(403, hideReview(catalogManager, author.reviewId).response.status)
        assertEquals(1, body(publicReviews(destination))["summary"]["total"].asInt())
    }
}
