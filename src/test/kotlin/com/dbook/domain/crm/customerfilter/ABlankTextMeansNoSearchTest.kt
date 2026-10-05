package com.dbook.domain.crm.customerfilter

import com.dbook.domain.crm.CustomerFilter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ABlankTextMeansNoSearchTest {
    @Test
    fun `given a blank text when building the filter then there is no search, and a text is trimmed`() {
        assertNull(CustomerFilter(text = "   ").searchText)
        assertEquals("maria", CustomerFilter(text = "  maria ").searchText)
    }
}
