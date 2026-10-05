package com.dbook.infrastructure.persistence.common

import com.dbook.domain.common.PageQuery
import com.dbook.domain.common.PageResult
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.sql.ResultSet

// The count and the page, in that order: a page past the end skips the second query. [pageSql] is complete: it
// carries its own ORDER BY and the `:limit` and `:offset` placeholders, because where the LIMIT goes matters (see
// CustomerSql.pageSql).
internal fun <T> NamedParameterJdbcTemplate.queryPage(
    countSql: String,
    pageSql: String,
    params: MapSqlParameterSource,
    page: PageQuery,
    mapper: (ResultSet) -> T,
): PageResult<T> {
    val total = queryForObject(countSql, params, Long::class.javaObjectType) ?: 0L
    if (page.offset >= total) {
        return PageResult(emptyList(), page, total)
    }
    params.addValue("limit", page.size).addValue("offset", page.offset)
    return PageResult(query(pageSql, params) { rs, _ -> mapper(rs) }, page, total)
}
