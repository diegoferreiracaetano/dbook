package com.dbook.presentation.common

/** One page of a list that only grows; `nextCursor` is opaque to the client and null on the last page. */
data class CursorPageResponse<T>(
    val items: List<T>,
    val nextCursor: String?,
)
