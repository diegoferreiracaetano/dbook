package com.dbook.presentation.favorite

import kotlin.test.Test
import kotlin.test.assertEquals

class ThePerCustomerLimitIsAConflictWithItsCodeTest : FavoriteApiFixture() {
    @Test
    fun `given 200 favorites when saving a new one then it is a 409 FAVORITES_LIMIT, a saved one still a 204`() {
        val (token, id) = aCustomer()
        jdbcTemplate.update(
            "INSERT INTO favorite (user_id, target_type, target_id, created_at) " +
                "SELECT ?, 'FLIGHT', (1000 + n)::text, now() FROM generate_series(1, 199) AS n",
            id,
        )
        assertEquals(204, save(token, "DESTINATION", "GRU").response.status)

        val refused = save(token, "DESTINATION", "GIG")

        assertEquals(409, refused.response.status)
        assertEquals("FAVORITES_LIMIT", body(refused)["code"].asText())
        assertEquals(204, save(token, "DESTINATION", "GRU").response.status)
        assertEquals(200, count(id))

        remove(token, "DESTINATION", "GRU")
        assertEquals(204, save(token, "DESTINATION", "GIG").response.status)
    }
}
