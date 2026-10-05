package com.dbook.presentation.crm

import com.dbook.application.crm.SearchCustomersQuery
import com.dbook.domain.common.PageQuery
import com.dbook.domain.common.SortDirection
import com.dbook.domain.crm.CustomerFilter
import com.dbook.domain.crm.CustomerSort
import com.dbook.domain.crm.CustomerSortField
import com.dbook.domain.identity.UserStatus
import java.time.Instant

data class CustomerSearchRequest(
    val query: String? = null,
    val status: UserStatus? = null,
    val createdFrom: Instant? = null,
    val createdTo: Instant? = null,
    val hasBookings: Boolean? = null,
    val sort: CustomerSortField = CustomerSortField.CREATED_AT,
    val direction: SortDirection = SortDirection.DESC,
    val page: Int = 0,
    val size: Int = PageQuery.DEFAULT_SIZE,
) {
    fun toQuery() =
        SearchCustomersQuery(
            filter = CustomerFilter(query, status, createdFrom, createdTo, hasBookings),
            sort = CustomerSort(sort, direction),
            page = PageQuery(page, size),
        )
}
