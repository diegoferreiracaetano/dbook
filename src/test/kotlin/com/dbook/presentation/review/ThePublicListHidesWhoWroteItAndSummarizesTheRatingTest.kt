package com.dbook.presentation.review

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ThePublicListHidesWhoWroteItAndSummarizesTheRatingTest : ReviewFixture() {
    @Test
    fun `given reviews when anyone reads then authors are a first name and an initial, and the rating is summed`() {
        val destination = newDestination()
        reviewer(destination, 5, "Loved it", name = "Maria Silva")
        reviewer(destination, 4, "Good", name = "João da Costa Pereira")
        reviewer(destination, 5, "Great", name = "Madonna")

        val result = publicReviews(destination)

        assertEquals(200, result.response.status)
        val text = result.response.getContentAsString(Charsets.UTF_8)
        val json = body(result)
        assertEquals(
            setOf("Maria S.", "João P.", "Madonna"),
            json["reviews"]["items"].map { it["author"].asText() }.toSet(),
        )
        assertFalse(text.contains("customerId") || text.contains("bookingId") || text.contains("@"))
        assertEquals(3, json["summary"]["total"].asInt())
        assertEquals(14.0 / 3, json["summary"]["average"].asDouble(), 1e-9)
        assertEquals(listOf(0, 0, 0, 1, 2), (1..5).map { json["summary"]["distribution"]["$it"].asInt() })
    }

    @Test
    fun `given a destination with no review when read then the average is null and the list is empty`() {
        val json = body(publicReviews(newDestination()))

        assertEquals(0, json["summary"]["total"].asInt())
        assertEquals(true, json["summary"]["average"].isNull)
        assertEquals(0, json["reviews"]["items"].size())
    }

    @Test
    fun `given a code that is not an IATA code when read then it is a 400`() {
        assertEquals(400, publicReviews("12").response.status)
    }

    @Test
    fun `given an anonymized customer when their review is read then no name is shown`() {
        val destination = newDestination()
        val who = reviewer(destination, 3, name = "Maria Silva")
        jdbcTemplate.update("UPDATE app_user SET anonymized_at = now() WHERE id = ?", who.customerId)

        assertEquals("Cliente anônimo", body(publicReviews(destination))["reviews"]["items"][0]["author"].asText())
    }
}
