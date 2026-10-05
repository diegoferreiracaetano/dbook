package com.dbook.domain.common

data class PageResult<T>(
    val items: List<T>,
    val query: PageQuery,
    val totalElements: Long,
) {
    val totalPages: Int get() = ((totalElements + query.size - 1) / query.size).toInt()
}
