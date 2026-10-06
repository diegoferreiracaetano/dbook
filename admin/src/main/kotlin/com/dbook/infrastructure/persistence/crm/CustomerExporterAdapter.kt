package com.dbook.infrastructure.persistence.crm

import com.dbook.domain.crm.CustomerExporter
import com.dbook.domain.crm.CustomerFilter
import com.dbook.domain.crm.CustomerSort
import com.dbook.domain.crm.CustomerSummary
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import javax.sql.DataSource

@Repository
class CustomerExporterAdapter(
    dataSource: DataSource,
) : CustomerExporter {
    // a fetch size makes the driver read a few rows at a time instead of the whole result: Postgres only streams
    // inside a transaction, which the @Transactional below provides on the same DataSource
    private val jdbc = NamedParameterJdbcTemplate(JdbcTemplate(dataSource).apply { fetchSize = FETCH_SIZE })

    @Transactional(readOnly = true)
    override fun export(
        filter: CustomerFilter,
        sort: CustomerSort,
        limit: Int,
        consumer: (CustomerSummary) -> Unit,
    ): Int {
        val params = MapSqlParameterSource().addValue("limit", limit)
        val where = CustomerSql.where(filter, params)
        var sent = 0
        jdbc.query(
            "${CustomerSql.SELECT_SUMMARY} WHERE $where ${CustomerSql.orderBy(sort)} LIMIT :limit",
            params,
        ) { rs ->
            consumer(CustomerSql.summaryOf(rs))
            sent++
        }
        return sent
    }

    private companion object {
        const val FETCH_SIZE = 500
    }
}
