package com.dbook.presentation.common

import com.dbook.domain.common.PageQuery

/** The `page` (from 0) and `size` query parameters of any paged list. */
data class PageParams(
    val page: Int = 0,
    val size: Int = PageQuery.DEFAULT_SIZE,
) {
    fun toQuery() = PageQuery(page, size)
}
