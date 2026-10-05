package com.dbook.presentation.favorite

import kotlin.test.Test
import kotlin.test.assertEquals

class FavoritesBelongToTheirOwnerTest : FavoriteApiFixture() {
    @Test
    fun `given the same destination saved by two customers when one removes it then the other keeps it`() {
        val (ana, _) = aCustomer()
        val (bob, _) = aCustomer()
        save(ana, "DESTINATION", "GRU")
        save(bob, "DESTINATION", "GRU")

        remove(ana, "DESTINATION", "GRU")

        assertEquals(emptyList(), listed(ana))
        assertEquals(listOf("DESTINATION:GRU"), listed(bob))
    }
}
