package com.dbook.application.booking

import com.dbook.domain.booking.AdminBookingDetail
import com.dbook.domain.booking.AdminBookingFilter
import com.dbook.domain.booking.AdminBookingReader
import com.dbook.domain.booking.AdminBookingSummary
import com.dbook.domain.booking.BookingNotFoundException
import com.dbook.domain.common.PageQuery
import com.dbook.domain.common.PageResult
import io.micrometer.observation.annotation.Observed
import org.springframework.stereotype.Service

@Observed(name = "dbook.usecase")
@Service
class SearchAdminBookingsUseCase(
    private val adminBookingReader: AdminBookingReader,
) {
    fun execute(
        filter: AdminBookingFilter,
        page: PageQuery,
    ): PageResult<AdminBookingSummary> = adminBookingReader.search(filter, page)
}

@Observed(name = "dbook.usecase")
@Service
class GetAdminBookingUseCase(
    private val adminBookingReader: AdminBookingReader,
) {
    fun execute(id: Long): AdminBookingDetail = adminBookingReader.find(id) ?: throw BookingNotFoundException(id)
}
