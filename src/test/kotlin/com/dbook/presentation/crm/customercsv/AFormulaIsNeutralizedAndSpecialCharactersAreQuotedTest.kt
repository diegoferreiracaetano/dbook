package com.dbook.presentation.crm.customercsv

import com.dbook.presentation.crm.CustomerCsv
import kotlin.test.Test
import kotlin.test.assertEquals

class AFormulaIsNeutralizedAndSpecialCharactersAreQuotedTest {
    @Test
    fun `given cells that start a formula when escaping then they become text with a leading quote`() {
        assertEquals("'+1", CustomerCsv.cell("+1"))
        assertEquals("'-1", CustomerCsv.cell("-1"))
        assertEquals("'@SUM(A1)", CustomerCsv.cell("@SUM(A1)"))
        assertEquals("'=1+1", CustomerCsv.cell("=1+1"))
        assertEquals("'\tcmd", CustomerCsv.cell("\tcmd"))
    }

    @Test
    fun `given commas, quotes and breaks when escaping then the cell is quoted with the quotes doubled`() {
        assertEquals("\"a,b\"", CustomerCsv.cell("a,b"))
        assertEquals("\"say \"\"hi\"\"\"", CustomerCsv.cell("say \"hi\""))
        assertEquals("\"one\ntwo\"", CustomerCsv.cell("one\ntwo"))
    }

    @Test
    fun `given a formula that also has a comma when escaping then it is neutralized and quoted`() {
        assertEquals("\"'=SUM(1,2)\"", CustomerCsv.cell("=SUM(1,2)"))
    }

    @Test
    fun `given plain and empty cells when escaping then they are untouched`() {
        assertEquals("plain", CustomerCsv.cell("plain"))
        assertEquals("", CustomerCsv.cell(""))
    }
}
