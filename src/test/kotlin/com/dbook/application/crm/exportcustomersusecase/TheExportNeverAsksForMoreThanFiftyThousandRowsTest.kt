package com.dbook.application.crm.exportcustomersusecase

import com.dbook.application.crm.CustomerExport
import com.dbook.application.crm.ExportCustomersCommand
import com.dbook.domain.common.access.Actor
import com.dbook.domain.common.access.Role
import com.dbook.domain.crm.CustomerFilter
import com.dbook.domain.crm.CustomerSort
import kotlin.test.Test
import kotlin.test.assertEquals

class TheExportNeverAsksForMoreThanFiftyThousandRowsTest : ExportCustomersUseCaseFixture() {
    @Test
    fun `given a prepared export when it runs then it reads once and with the cap`() {
        val export =
            useCase.execute(
                ExportCustomersCommand(Actor(1, Role.SUPER_ADMIN), CustomerFilter(), CustomerSort()),
            )

        export.forEach { }

        assertEquals(1, exporter.calls)
        assertEquals(CustomerExport.MAX_ROWS, exporter.lastLimit)
        assertEquals(50_000, CustomerExport.MAX_ROWS)
    }
}
