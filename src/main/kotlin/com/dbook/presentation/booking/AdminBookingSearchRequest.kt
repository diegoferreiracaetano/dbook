package com.dbook.presentation.booking

import com.dbook.domain.booking.AdminBookingFilter
import com.dbook.domain.booking.BookingStatus
import com.dbook.domain.common.PageQuery
import java.time.Instant

/** The query string of `GET /admin/bookings`: every filter is optional. */
data class AdminBookingSearchRequest(
    val status: BookingStatus? = null,
    val bookableId: Long? = null,
    val customerId: Long? = null,
    val createdFrom: Instant? = null,
    val createdTo: Instant? = null,
    val paid: Boolean? = null,
    val page: Int = 0,
    val size: Int = PageQuery.DEFAULT_SIZE,
) {
    fun toFilter() = AdminBookingFilter(status, bookableId, customerId, createdFrom, createdTo, paid)

    fun toPage() = PageQuery(page, size)
}
