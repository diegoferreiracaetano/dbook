package com.dbook.presentation.favorite

import kotlin.test.Test
import kotlin.test.assertEquals

class SavingAndRemovingAreIdempotentAndListedNewestFirstTest : FavoriteApiFixture() {
    @Test
    fun `given a destination and a flight when saved twice then each is one favorite and the list is newest first`() {
        val (token, id) = aCustomer()
        val (flight) = registerFlightWithOneSeat()

        assertEquals(204, save(token, "DESTINATION", "GRU").response.status)
        assertEquals(204, save(token, "DESTINATION", "GRU").response.status)
        assertEquals(204, save(token, "FLIGHT", "$flight").response.status)
        assertEquals(204, save(token, "FLIGHT", "$flight").response.status)

        assertEquals(2, count(id))
        assertEquals(listOf("FLIGHT:$flight", "DESTINATION:GRU"), listed(token))
        assertEquals(listOf("DESTINATION:GRU"), listed(token, "type" to "DESTINATION"))
        assertEquals(listOf("FLIGHT:$flight"), listed(token, "type" to "FLIGHT"))
    }

    @Test
    fun `given a favorite when removed twice then it is gone and both calls are a 204`() {
        val (token, id) = aCustomer()
        save(token, "DESTINATION", "GIG")

        assertEquals(204, remove(token, "DESTINATION", "GIG").response.status)
        assertEquals(204, remove(token, "DESTINATION", "GIG").response.status)

        assertEquals(0, count(id))
    }

    @Test
    fun `given four favorites when paging by two then the pages continue and the total is right`() {
        val (token, _) = aCustomer()
        listOf("GRU", "GIG").forEach { save(token, "DESTINATION", it) }
        repeat(2) { save(token, "FLIGHT", "${registerFlightWithOneSeat().first}") }

        val first = body(list(token, "size" to "2", "page" to "0"))
        val second = body(list(token, "size" to "2", "page" to "1"))

        assertEquals(4, first["totalElements"].asInt())
        assertEquals(2, first["items"].size())
        assertEquals(2, second["items"].size())
        assertEquals(4, (first["items"] + second["items"]).map { it["id"].asText() }.toSet().size)
    }
}
