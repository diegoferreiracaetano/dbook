package com.dbook.presentation.catalog.csvparser

import com.dbook.presentation.catalog.CsvParser
import kotlin.test.Test
import kotlin.test.assertFailsWith

class AFileThatEndsInsideAQuoteIsRefusedTest {
    @Test
    fun `given an unclosed quote when parsing then it is refused`() {
        assertFailsWith<IllegalArgumentException> { CsvParser.parse("a,b\n\"never closed,1\n") }
    }
}
