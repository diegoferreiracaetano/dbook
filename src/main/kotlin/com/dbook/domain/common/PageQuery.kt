package com.dbook.domain.common

data class PageQuery(
    val page: Int = 0,
    val size: Int = DEFAULT_SIZE,
) {
    init {
        require(page >= 0) { "page must not be negative" }
        require(size in 1..MAX_SIZE) { "size must be between 1 and $MAX_SIZE" }
    }

    val offset: Long get() = page.toLong() * size

    companion object {
        const val DEFAULT_SIZE = 20
        const val MAX_SIZE = 100
    }
}
