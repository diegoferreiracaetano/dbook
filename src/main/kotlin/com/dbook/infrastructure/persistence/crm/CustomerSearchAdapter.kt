package com.dbook.infrastructure.persistence.crm

import com.dbook.domain.common.PageQuery
import com.dbook.domain.common.PageResult
import com.dbook.domain.crm.CustomerFilter
import com.dbook.domain.crm.CustomerSearch
import com.dbook.domain.crm.CustomerSort
import com.dbook.domain.crm.CustomerSummary
import com.dbook.infrastructure.persistence.common.queryPage
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class CustomerSearchAdapter(
    private val jdbc: NamedParameterJdbcTemplate,
) : CustomerSearch {
    @Transactional(readOnly = true)
    override fun search(
        filter: CustomerFilter,
        sort: CustomerSort,
        page: PageQuery,
    ): PageResult<CustomerSummary> {
        val params = MapSqlParameterSource()
        val where = CustomerSql.where(filter, params)
        return jdbc.queryPage(
            countSql = "SELECT count(*) FROM app_user u WHERE $where",
            pageSql = CustomerSql.pageSql(where, sort),
            params = params,
            page = page,
            mapper = CustomerSql::summaryOf,
        )
    }
}
