package com.dbook.application.catalog

import com.dbook.domain.catalog.AdminFlightDetail
import com.dbook.domain.catalog.AdminFlightFilter
import com.dbook.domain.catalog.AdminFlightReader
import com.dbook.domain.catalog.AdminFlightSummary
import com.dbook.domain.catalog.FlightNotFoundException
import com.dbook.domain.common.PageQuery
import com.dbook.domain.common.PageResult
import io.micrometer.observation.annotation.Observed
import org.springframework.stereotype.Service

@Observed(name = "dbook.usecase")
@Service
class SearchAdminFlightsUseCase(
    private val adminFlightReader: AdminFlightReader,
) {
    fun execute(
        filter: AdminFlightFilter,
        page: PageQuery,
    ): PageResult<AdminFlightSummary> = adminFlightReader.search(filter, page)
}

@Observed(name = "dbook.usecase")
@Service
class GetAdminFlightUseCase(
    private val adminFlightReader: AdminFlightReader,
) {
    fun execute(id: Long): AdminFlightDetail = adminFlightReader.find(id) ?: throw FlightNotFoundException(id)
}
