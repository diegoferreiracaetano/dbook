package com.dbook.presentation.crm

import com.dbook.application.crm.CustomerExport
import com.dbook.domain.crm.CustomerSummary
import java.io.OutputStream

// A spreadsheet runs a cell that starts with = + - @ (or a tab or return) as a formula, so a customer who registered
// as "=HYPERLINK(...)" would attack whoever opens the export. Those cells get a leading quote, which makes them text.
object CustomerCsv {
    private const val BOM = "\uFEFF"
    private val formulaStarts = setOf('=', '+', '-', '@', '\t', '\r')
    private val mustBeQuoted = setOf(',', '"', '\n', '\r')
    private val header = listOf("id", "name", "email", "status", "createdAt", "lastLoginAt", "bookingCount")

    fun cell(value: String): String {
        val safe = if (value.isNotEmpty() && value.first() in formulaStarts) "'$value" else value
        return if (safe.any { it in mustBeQuoted }) "\"" + safe.replace("\"", "\"\"") + "\"" else safe
    }

    fun row(customer: CustomerSummary): String =
        listOf(
            customer.id.toString(),
            cell(customer.name),
            cell(customer.email),
            customer.status.name,
            customer.createdAt.toString(),
            customer.lastLoginAt?.toString().orEmpty(),
            customer.bookingCount.toString(),
        ).joinToString(",")

    /** Writes the whole file and returns how many customers it holds. The BOM makes Excel read it as UTF-8. */
    fun write(
        out: OutputStream,
        export: CustomerExport,
    ): Int {
        val writer = out.bufferedWriter(Charsets.UTF_8)
        writer.write(BOM + header.joinToString(",") + "\r\n")
        val count = export.forEach { writer.write(row(it) + "\r\n") }
        writer.flush()
        return count
    }
}
