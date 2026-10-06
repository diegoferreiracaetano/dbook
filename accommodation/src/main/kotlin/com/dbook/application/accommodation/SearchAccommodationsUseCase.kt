package com.dbook.application.accommodation

import com.dbook.domain.accommodation.AccommodationResult
import com.dbook.domain.accommodation.AccommodationSearch
import com.dbook.domain.accommodation.AccommodationSearcher
import com.dbook.domain.common.PageQuery
import com.dbook.domain.common.PageResult
import io.micrometer.observation.annotation.Observed
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.LocalDate

/** Public search: hotels at a destination with a room free for every night, for these guests, cheapest stay first. */
@Observed(name = "dbook.usecase")
@Service
class SearchAccommodationsUseCase(
    private val searcher: AccommodationSearcher,
    private val clock: Clock,
) {
    fun execute(
        search: AccommodationSearch,
        page: PageQuery,
    ): PageResult<AccommodationResult> {
        require(!search.checkIn.isBefore(LocalDate.now(clock))) { "checkIn must not be in the past" }
        require(search.nights <= MAX_NIGHTS) { "a stay is at most $MAX_NIGHTS nights" }
        require(search.guests <= MAX_GUESTS) { "guests must be at most $MAX_GUESTS" }
        return searcher.search(search, page)
    }

    private companion object {
        const val MAX_NIGHTS = 30L
        const val MAX_GUESTS = 10
    }
}
