package com.dbook.application.crm.exportcustomersusecase

import com.dbook.application.audit.FakeAuditLog
import com.dbook.application.crm.ExportCustomersUseCase
import com.dbook.domain.crm.CustomerExporter
import com.dbook.domain.crm.CustomerFilter
import com.dbook.domain.crm.CustomerSort
import com.dbook.domain.crm.CustomerSummary

class RecordingExporter : CustomerExporter {
    var calls = 0
    var lastLimit: Int? = null
    var lastFilter: CustomerFilter? = null

    override fun export(
        filter: CustomerFilter,
        sort: CustomerSort,
        limit: Int,
        consumer: (CustomerSummary) -> Unit,
    ): Int {
        calls++
        lastLimit = limit
        lastFilter = filter
        return 0
    }
}

abstract class ExportCustomersUseCaseFixture {
    protected val audit = FakeAuditLog()
    protected val exporter = RecordingExporter()
    protected val useCase = ExportCustomersUseCase(exporter, audit)
}
