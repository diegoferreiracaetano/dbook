package com.dbook.domain.common.pagequery

import com.dbook.domain.common.PageQuery
import kotlin.test.Test
import kotlin.test.assertFailsWith

class RejectsASizeOutsideOneToAHundredTest {
    @Test
    fun `given a size of 0 or 101 when building the query then it is refused, and a negative page too`() {
        assertFailsWith<IllegalArgumentException> { PageQuery(size = 0) }
        assertFailsWith<IllegalArgumentException> { PageQuery(size = 101) }
        assertFailsWith<IllegalArgumentException> { PageQuery(page = -1) }
    }
}
