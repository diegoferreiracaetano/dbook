package com.dbook.presentation.customerprofile

import com.dbook.QueryCounter
import com.dbook.domain.common.PageQuery
import com.dbook.domain.crm.CustomerFilter
import com.dbook.domain.crm.CustomerSearch
import com.dbook.domain.crm.CustomerSort
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.Test
import kotlin.test.assertEquals

class TheSearchCostsTwoQueriesHoweverManyCustomersTest : CustomerProfileFixture() {
    @Autowired
    lateinit var queryCounter: QueryCounter

    @Autowired
    lateinit var customerSearch: CustomerSearch

    @Test
    fun `given five customers with bookings when searching then it costs the count and the page only`() {
        val tag = newTag()
        repeat(5) { bookSeats(registerAndLogin(uniqueEmail(), name = "N$it $tag"), 1) }

        var found = 0
        val cost =
            queryCounter.queriesOf {
                found =
                    customerSearch.search(CustomerFilter(text = tag), CustomerSort(), PageQuery(size = 20)).items.size
            }

        assertEquals(5, found)
        assertEquals(2, cost)
    }
}
