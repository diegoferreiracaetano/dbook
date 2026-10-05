package com.dbook.domain.common.pagequery

import com.dbook.domain.common.PageQuery
import com.dbook.domain.common.PageResult
import kotlin.test.Test
import kotlin.test.assertEquals

class TheLastPartialPageCountsTest {
    @Test
    fun `given 45 elements and pages of 20 when counting pages then there are 3, and none when empty`() {
        assertEquals(3, PageResult(emptyList<Int>(), PageQuery(size = 20), totalElements = 45).totalPages)
        assertEquals(0, PageResult(emptyList<Int>(), PageQuery(size = 20), totalElements = 0).totalPages)
    }
}
