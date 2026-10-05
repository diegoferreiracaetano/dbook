package com.dbook.presentation.catalogadmin

import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import kotlin.test.Test
import kotlin.test.assertEquals

class TwoEditsAtTheSameTimeOnlyOneWinsTest : CatalogAdminFixture() {
    @Test
    fun `given two editors saving at once with the same version when racing then one is 200 and the other 409`() {
        val token = manager()
        val id = createFlight(token)
        val version = versionOf(token, id)
        val barrier = CyclicBarrier(2)
        val pool = Executors.newFixedThreadPool(2)

        val statuses =
            try {
                listOf(110.0, 120.0).map { price ->
                    pool.submit<Int> {
                        barrier.await()
                        edit(token, id, mapOf("price" to price), version = version).response.status
                    }
                }.map { it.get() }
            } finally {
                pool.shutdown()
            }

        assertEquals(listOf(200, 409), statuses.sorted())
    }
}
