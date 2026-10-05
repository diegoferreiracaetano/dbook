package com.dbook.domain.crm

import com.dbook.domain.common.PageQuery
import com.dbook.domain.common.PageResult

interface CustomerNoteRepository {
    fun save(note: CustomerNote): CustomerNote

    /** The note only if it belongs to [customerId] and was not deleted. */
    fun findActive(
        customerId: Long,
        noteId: Long,
    ): CustomerNote?

    /** Pinned first, then newest first. */
    fun findActiveByCustomer(
        customerId: Long,
        page: PageQuery,
    ): PageResult<CustomerNote>
}
