package com.dbook.domain.accommodation

import com.dbook.domain.common.PageQuery
import com.dbook.domain.common.PageResult

interface AdminAccommodationReader {
    /** Every hotel, newest first, active or not; [destinationIataCode] narrows to one destination. */
    fun search(
        destinationIataCode: String?,
        page: PageQuery,
    ): PageResult<Accommodation>
}
