package com.dbook.presentation.favorite

import org.springframework.test.web.servlet.get
import kotlin.test.Test
import kotlin.test.assertEquals

class ATargetThatDoesNotExistOrIsMalformedIsRefusedTest : FavoriteApiFixture() {
    @Test
    fun `given a destination or a flight that does not exist when saving then it is a 404 and nothing is stored`() {
        val (token, id) = aCustomer()

        assertEquals(404, save(token, "DESTINATION", "QQQ").response.status)
        assertEquals(404, save(token, "FLIGHT", "999999999").response.status)

        assertEquals(0, count(id))
    }

    @Test
    fun `given a malformed id or an unknown type when saving then it is a 400`() {
        val (token, _) = aCustomer()

        assertEquals(400, save(token, "DESTINATION", "gru").response.status)
        assertEquals(400, save(token, "DESTINATION", "GRUU").response.status)
        assertEquals(400, save(token, "FLIGHT", "abc").response.status)
        assertEquals(400, save(token, "HOTEL", "1").response.status)
        assertEquals(400, list(token, "type" to "HOTEL").response.status)
    }

    @Test
    fun `given no token when using favorites then it is a 401`() {
        assertEquals(401, mockMvc.get("/v1/favorites").andReturn().response.status)
    }
}
