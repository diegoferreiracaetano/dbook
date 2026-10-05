package com.dbook.application.crm

import com.dbook.domain.audit.AuditAction
import com.dbook.domain.audit.AuditEvent
import com.dbook.domain.audit.AuditLog
import com.dbook.domain.crm.CustomerExporter
import com.dbook.domain.crm.CustomerFilter
import com.dbook.domain.crm.CustomerSort
import com.dbook.domain.crm.CustomerSummary
import com.dbook.domain.identity.Actor
import io.micrometer.observation.annotation.Observed
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class ExportCustomersCommand(
    val actor: Actor,
    val filter: CustomerFilter,
    val sort: CustomerSort,
)

/** The export, ready to run: nothing is read until [forEach] is called, and it never sends more than the cap. */
class CustomerExport(
    private val exporter: CustomerExporter,
    private val filter: CustomerFilter,
    private val sort: CustomerSort,
) {
    fun forEach(consumer: (CustomerSummary) -> Unit): Int = exporter.export(filter, sort, MAX_ROWS, consumer)

    companion object {
        const val MAX_ROWS = 50_000
    }
}

// A bulk copy of personal data. The trail is written first, with the filter that was used, so that the export
// cannot happen without leaving a record, and only then does the caller start reading.
@Observed(name = "dbook.usecase")
@Service
class ExportCustomersUseCase(
    private val exporter: CustomerExporter,
    private val auditLog: AuditLog,
) {
    @Transactional
    fun execute(command: ExportCustomersCommand): CustomerExport {
        auditLog.record(
            AuditEvent(
                actor = command.actor,
                action = AuditAction.CUSTOMER_EXPORTED,
                targetId = EXPORT_TARGET,
                after = snapshotOf(command),
            ),
        )
        return CustomerExport(exporter, command.filter, command.sort)
    }

    private fun snapshotOf(command: ExportCustomersCommand): Map<String, Any?> =
        mapOf(
            "text" to command.filter.searchText,
            "status" to command.filter.status?.name,
            "createdFrom" to command.filter.createdFrom?.toString(),
            "createdTo" to command.filter.createdTo?.toString(),
            "hasBookings" to command.filter.hasBookings,
            "sort" to command.sort.field.name,
            "direction" to command.sort.direction.name,
            "maxRows" to CustomerExport.MAX_ROWS,
        )

    private companion object {
        const val EXPORT_TARGET = "customers"
    }
}
