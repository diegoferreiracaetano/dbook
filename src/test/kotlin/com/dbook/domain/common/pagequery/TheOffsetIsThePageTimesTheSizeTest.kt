package com.dbook.domain.common.pagequery

import com.dbook.domain.common.PageQuery
import kotlin.test.Test
import kotlin.test.assertEquals

class TheOffsetIsThePageTimesTheSizeTest {
    @Test
    fun `given page 3 of size 20 when the offset is read then it is 60`() {
        assertEquals(60L, PageQuery(page = 3, size = 20).offset)
    }
}
