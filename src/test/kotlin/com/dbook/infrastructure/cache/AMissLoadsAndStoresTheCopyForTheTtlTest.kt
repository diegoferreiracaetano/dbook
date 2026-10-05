package com.dbook.infrastructure.cache

import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals

class AMissLoadsAndStoresTheCopyForTheTtlTest : RedisDashboardCacheFixture() {
    @Test
    fun `given no copy when asking then it loads, stores it for 60 seconds and counts a miss`() {
        `when`(values.get("dbook:dashboard:k")).thenReturn(null)

        val answer = cache.remember("k", Sample::class.java, ::load)

        assertEquals(Sample("fresh", 7), answer)
        assertEquals(1, loads)
        verify(values).set("dbook:dashboard:k", """{"name":"fresh","total":7}""", Duration.ofSeconds(60))
        assertEquals(1.0, counted("miss"))
    }
}
