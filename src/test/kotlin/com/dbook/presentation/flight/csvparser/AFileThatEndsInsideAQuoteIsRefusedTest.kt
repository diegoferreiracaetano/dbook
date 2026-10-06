package com.dbook.presentation.flight.csvparser

import com.dbook.presentation.flight.CsvParser
import kotlin.test.Test
import kotlin.test.assertFailsWith

class AFileThatEndsInsideAQuoteIsRefusedTest {
    @Test
    fun `given an unclosed quote when parsing then it is refused`() {
        assertFailsWith<IllegalArgumentException> { CsvParser.parse("a,b\n\"never closed,1\n") }
    }
}
