package com.dbook.presentation.crm

import com.dbook.application.crm.ExportCustomersCommand
import com.dbook.domain.common.SortDirection
import com.dbook.domain.crm.CustomerFilter
import com.dbook.domain.crm.CustomerSort
import com.dbook.domain.crm.CustomerSortField
import com.dbook.domain.identity.Actor
import com.dbook.domain.identity.UserStatus
import java.time.Instant

/** The query string of `GET /admin/customers/export`: the filters of the search, without paging. */
data class CustomerExportRequest(
    val query: String? = null,
    val status: UserStatus? = null,
    val createdFrom: Instant? = null,
    val createdTo: Instant? = null,
    val hasBookings: Boolean? = null,
    val sort: CustomerSortField = CustomerSortField.CREATED_AT,
    val direction: SortDirection = SortDirection.DESC,
) {
    fun toCommand(actor: Actor) =
        ExportCustomersCommand(
            actor = actor,
            filter = CustomerFilter(query, status, createdFrom, createdTo, hasBookings),
            sort = CustomerSort(sort, direction),
        )
}
