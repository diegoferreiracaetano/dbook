package com.dbook.application.crm

import com.dbook.domain.common.PageQuery
import com.dbook.domain.common.PageResult
import com.dbook.domain.crm.CustomerFilter
import com.dbook.domain.crm.CustomerSearch
import com.dbook.domain.crm.CustomerSort
import com.dbook.domain.crm.CustomerSummary
import io.micrometer.observation.annotation.Observed
import org.springframework.stereotype.Service

data class SearchCustomersQuery(
    val filter: CustomerFilter = CustomerFilter(),
    val sort: CustomerSort = CustomerSort(),
    val page: PageQuery = PageQuery(),
)

@Observed(name = "dbook.usecase")
@Service
class SearchCustomersUseCase(
    private val customerSearch: CustomerSearch,
) {
    fun execute(query: SearchCustomersQuery): PageResult<CustomerSummary> =
        customerSearch.search(query.filter, query.sort, query.page)
}
