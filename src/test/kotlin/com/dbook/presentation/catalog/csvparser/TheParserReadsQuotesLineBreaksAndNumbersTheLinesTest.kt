package com.dbook.presentation.catalog.csvparser

import com.dbook.presentation.catalog.CsvParser
import kotlin.test.Test
import kotlin.test.assertEquals

class TheParserReadsQuotesLineBreaksAndNumbersTheLinesTest {
    @Test
    fun `given quoted cells with commas, quotes and line breaks when parsing then they are one cell each`() {
        val records = CsvParser.parse("a,b\r\n\"x,1\",\"say \"\"hi\"\"\"\n\n\"two\nlines\",z\n")

        assertEquals(listOf("a", "b"), records[0].cells)
        assertEquals(listOf("x,1", "say \"hi\""), records[1].cells)
        assertEquals(listOf("two\nlines", "z"), records[2].cells)
        assertEquals(listOf(1, 2, 4), records.map { it.line })
    }

    @Test
    fun `given a byte order mark and an empty trailing cell when parsing then the mark is dropped and the cell kept`() {
        val records = CsvParser.parse("\uFEFFa,b\n1,\n")

        assertEquals(listOf("a", "b"), records[0].cells)
        assertEquals(listOf("1", ""), records[1].cells)
    }
}
