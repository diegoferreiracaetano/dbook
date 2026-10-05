package com.dbook.domain.review

import kotlin.test.Test
import kotlin.test.assertEquals

class ThePublicAuthorNameShowsOnlyTheFirstNameAndTheLastInitialTest {
    @Test
    fun `given full names when shown then it is the first name and the initial of the last one`() {
        assertEquals("Maria S.", publicAuthorName("Maria Silva", anonymized = false))
        assertEquals("João P.", publicAuthorName("  João da Costa Pereira ", anonymized = false))
        assertEquals("Madonna", publicAuthorName("Madonna", anonymized = false))
    }

    @Test
    fun `given an anonymized customer or an empty name when shown then there is no name to show`() {
        assertEquals("Cliente anônimo", publicAuthorName("Maria Silva", anonymized = true))
        assertEquals("Cliente anônimo", publicAuthorName("   ", anonymized = false))
    }
}
