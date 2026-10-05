package com.dbook.domain.payment

import com.dbook.domain.common.PageQuery
import com.dbook.domain.common.PageResult

interface RefundRepository {
    /** @throws RefundAlreadyRequestedException if the booking already has a live (not FAILED) refund. */
    fun save(refund: Refund): Refund

    fun findById(id: Long): Refund?

    /** The refund of the booking that is still being made (REQUESTED), if any. */
    fun findInProgressByBookingId(bookingId: Long): Refund?

    fun findByRequestedByAndIdempotencyKey(
        requestedBy: Long,
        idempotencyKey: String,
    ): Refund?

    /** Newest first; [status] null means every status. */
    fun search(
        status: RefundStatus?,
        page: PageQuery,
    ): PageResult<Refund>
}
