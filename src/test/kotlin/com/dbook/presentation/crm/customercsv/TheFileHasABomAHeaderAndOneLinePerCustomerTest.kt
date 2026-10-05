package com.dbook.presentation.crm.customercsv

import com.dbook.application.crm.CustomerExport
import com.dbook.domain.crm.CustomerExporter
import com.dbook.domain.crm.CustomerFilter
import com.dbook.domain.crm.CustomerSort
import com.dbook.domain.crm.CustomerSummary
import com.dbook.domain.identity.UserStatus
import com.dbook.presentation.crm.CustomerCsv
import java.io.ByteArrayOutputStream
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class TheFileHasABomAHeaderAndOneLinePerCustomerTest {
    private val january = Instant.parse("2026-01-01T00:00:00Z")

    private class FixedExporter(private val customers: List<CustomerSummary>) : CustomerExporter {
        override fun export(
            filter: CustomerFilter,
            sort: CustomerSort,
            limit: Int,
            consumer: (CustomerSummary) -> Unit,
        ): Int {
            customers.forEach(consumer)
            return customers.size
        }
    }

    @Test
    fun `given two customers when writing the file then BOM, header and two lines, the second with no last login`() {
        val customers =
            listOf(
                CustomerSummary(1, "Ana", "ana@example.com", UserStatus.ACTIVE, january, january.plusSeconds(60), 2),
                CustomerSummary(2, "=cmd", "b@example.com", UserStatus.BLOCKED, january, null, 0),
            )
        val out = ByteArrayOutputStream()

        val written = CustomerCsv.write(out, CustomerExport(FixedExporter(customers), CustomerFilter(), CustomerSort()))

        assertEquals(2, written)
        assertEquals(
            "\uFEFFid,name,email,status,createdAt,lastLoginAt,bookingCount\r\n" +
                "1,Ana,ana@example.com,ACTIVE,2026-01-01T00:00:00Z,2026-01-01T00:01:00Z,2\r\n" +
                "2,'=cmd,b@example.com,BLOCKED,2026-01-01T00:00:00Z,,0\r\n",
            out.toString(Charsets.UTF_8),
        )
    }
}
