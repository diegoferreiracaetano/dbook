package com.dbook.presentation.lifecycle

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.get
import kotlin.test.Test
import kotlin.test.assertEquals

class ANewVersionAnswersAlongsideTheOldOneTest : LifecycleFixture() {
    @Autowired
    lateinit var objectMapper: ObjectMapper

    private fun destinations(path: String) =
        objectMapper.readTree(mockMvc.get(path).andReturn().response.contentAsString)

    @Test
    fun `given the same data when read from v1 and from v2 then each answers in its own shape, both at once`() {
        val v1 = destinations("/v1/destinations")
        val v2 = destinations("/v2/destinations")

        assertEquals(v1.size(), v2.size())
        val one = v1.first()
        val two = v2.first { it["iataCode"].asText() == one["iataCode"].asText() }
        assertEquals(true, one.has("averageRating") && one.has("lowestPrice"))
        assertEquals(false, two.has("averageRating") || two.has("lowestPrice"))
        assertEquals(one["averageRating"], two["rating"]["average"])
        assertEquals(one["lowestPrice"], two["price"]["lowest"])
    }
}
