package com.dbook.domain.crm

import com.dbook.domain.common.PageQuery
import com.dbook.domain.common.PageResult
import com.dbook.domain.common.SortDirection
import com.dbook.domain.identity.UserStatus
import java.time.Instant

data class CustomerFilter(
    val text: String? = null,
    val status: UserStatus? = null,
    val createdFrom: Instant? = null,
    val createdTo: Instant? = null,
    val hasBookings: Boolean? = null,
) {
    val searchText: String? = text?.trim()?.takeIf { it.isNotEmpty() }

    init {
        require((searchText?.length ?: 0) <= MAX_TEXT_LENGTH) {
            "query must have at most $MAX_TEXT_LENGTH characters"
        }
        require(createdFrom == null || createdTo == null || !createdTo.isBefore(createdFrom)) {
            "createdTo must not be before createdFrom"
        }
    }

    companion object {
        const val MAX_TEXT_LENGTH = 100
    }
}

enum class CustomerSortField { NAME, EMAIL, CREATED_AT, LAST_LOGIN_AT }

data class CustomerSort(
    val field: CustomerSortField = CustomerSortField.CREATED_AT,
    val direction: SortDirection = SortDirection.DESC,
)

data class CustomerSummary(
    val id: Long,
    val name: String,
    val email: String,
    val status: UserStatus,
    val createdAt: Instant,
    val lastLoginAt: Instant?,
    val bookingCount: Long,
)

interface CustomerSearch {
    fun search(
        filter: CustomerFilter,
        sort: CustomerSort,
        page: PageQuery,
    ): PageResult<CustomerSummary>
}
