package com.dbook.infrastructure.persistence.crm

import com.dbook.domain.crm.CustomerExporter
import com.dbook.domain.crm.CustomerFilter
import com.dbook.domain.crm.CustomerSort
import com.dbook.presentation.customersearch.CustomerSearchFixture
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.Test
import kotlin.test.assertEquals

class TheExporterStopsAtTheLimitTest : CustomerSearchFixture() {
    @Autowired
    lateinit var exporter: CustomerExporter

    @Test
    fun `given three customers when exporting with a limit of two then two are sent`() {
        val tag = newTag()
        listOf("A", "B", "C").forEach { newCustomer("$it $tag") }
        val names = mutableListOf<String>()

        val sent = exporter.export(CustomerFilter(text = tag), CustomerSort(), limit = 2) { names += it.name }

        assertEquals(2, sent)
        assertEquals(2, names.size)
    }
}
