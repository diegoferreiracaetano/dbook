package com.dbook.presentation.customerhistory

import com.dbook.QueryCounter
import com.dbook.domain.common.PageQuery
import com.dbook.domain.crm.CustomerHistory
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.Test
import kotlin.test.assertEquals

class EachListCostsTwoQueriesHoweverLongTheHistoryIsTest : CustomerHistoryFixture() {
    @Autowired
    lateinit var queryCounter: QueryCounter

    @Autowired
    lateinit var customerHistory: CustomerHistory

    @Test
    fun `given a customer with four paid reviewed bookings when listing each history then it costs count plus page`() {
        val email = uniqueEmail()
        val token = registerAndLogin(email)
        bookSeats(token, 4).forEach {
            pay(token, it)
            review(token, it, rating = 3)
        }
        val id = userIdOf(email)
        val page = PageQuery(size = 20)

        assertEquals(2, queryCounter.queriesOf { customerHistory.bookings(id, page) })
        assertEquals(2, queryCounter.queriesOf { customerHistory.payments(id, page) })
        assertEquals(2, queryCounter.queriesOf { customerHistory.reviews(id, page) })
    }
}
