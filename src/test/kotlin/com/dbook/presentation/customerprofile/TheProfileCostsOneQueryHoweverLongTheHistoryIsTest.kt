package com.dbook.presentation.customerprofile

import com.dbook.QueryCounter
import com.dbook.domain.crm.CustomerProfileReader
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.Test
import kotlin.test.assertEquals

class TheProfileCostsOneQueryHoweverLongTheHistoryIsTest : CustomerProfileFixture() {
    @Autowired
    lateinit var queryCounter: QueryCounter

    @Autowired
    lateinit var reader: CustomerProfileReader

    @Test
    fun `given a customer with one booking and another with four when reading profiles then each costs one query`() {
        val shortEmail = uniqueEmail()
        bookSeats(registerAndLogin(shortEmail), 1)
        val longEmail = uniqueEmail()
        bookSeats(registerAndLogin(longEmail), 4)

        val shortCost = queryCounter.queriesOf { reader.find(userIdOf(shortEmail)) }
        val longCost = queryCounter.queriesOf { reader.find(userIdOf(longEmail)) }

        // userIdOf runs one more query inside the block: the profile itself is the other one
        assertEquals(2, shortCost)
        assertEquals(shortCost, longCost)
    }
}
