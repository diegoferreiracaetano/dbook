package com.dbook.infrastructure.cache

import org.mockito.Mockito.`when`
import kotlin.test.Test
import kotlin.test.assertEquals

class AHitReturnsTheCopyWithoutLoadingTest : RedisDashboardCacheFixture() {
    @Test
    fun `given a copy in Redis when asking then it is returned, nothing is loaded and a hit is counted`() {
        `when`(values.get("dbook:dashboard:k")).thenReturn("""{"name":"cached","total":3}""")

        val answer = cache.remember("k", Sample::class.java, ::load)

        assertEquals(Sample("cached", 3), answer)
        assertEquals(0, loads)
        assertEquals(1.0, counted("hit"))
    }
}
