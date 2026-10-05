package com.dbook.domain.crm.customerfilter

import com.dbook.domain.crm.CustomerFilter
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertFailsWith

class RejectsATooLongTextOrAnInvertedPeriodTest {
    @Test
    fun `given a 101 character text or an inverted period when building the filter then it is refused`() {
        assertFailsWith<IllegalArgumentException> { CustomerFilter(text = "a".repeat(101)) }
        assertFailsWith<IllegalArgumentException> {
            CustomerFilter(
                createdFrom = Instant.parse("2026-02-01T00:00:00Z"),
                createdTo = Instant.parse("2026-01-01T00:00:00Z"),
            )
        }
    }
}
