package com.dbook.presentation.common

import com.dbook.domain.common.PageResult

data class PageResponse<T>(
    val items: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
) {
    companion object {
        fun <S, T> from(
            result: PageResult<S>,
            transform: (S) -> T,
        ) = PageResponse(
            items = result.items.map(transform),
            page = result.query.page,
            size = result.query.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
        )
    }
}
