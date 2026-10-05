package com.dbook.infrastructure.cache

import org.mockito.Mockito.`when`
import kotlin.test.Test
import kotlin.test.assertEquals

class AnUnreadableCopyIsTreatedAsMissingTest : RedisDashboardCacheFixture() {
    @Test
    fun `given a copy that is not JSON of the type when asking then it is recomputed`() {
        `when`(values.get("dbook:dashboard:k")).thenReturn("not json at all")

        val answer = cache.remember("k", Sample::class.java, ::load)

        assertEquals(Sample("fresh", 7), answer)
        assertEquals(1, loads)
    }
}
