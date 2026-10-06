package com.dbook.infrastructure.cache

import com.dbook.QueryCounter
import com.dbook.application.flight.SearchFlightsUseCase
import com.dbook.domain.flight.Flight
import com.dbook.presentation.catalogadmin.CatalogAdminFixture
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.TestPropertySource
import java.time.LocalDateTime

// the other tests run with the search cache off (see src/test/resources/application.properties): they book and then
// search expecting to see the seat gone at once, which a cache that keeps copies for seconds would not show
@TestPropertySource(properties = ["flight-search-cache.enabled=true"])
abstract class FlightSearchCacheFixture : CatalogAdminFixture() {
    @Autowired
    lateinit var search: SearchFlightsUseCase

    @Autowired
    lateinit var queryCounter: QueryCounter

    @Autowired
    lateinit var meters: MeterRegistry

    protected fun searchOn(departure: LocalDateTime): List<Flight> =
        search.execute("GRU", "GIG", departure.toLocalDate())

    protected fun hits(): Double = meters.counter("dbook.cache", "cache", "flight-search", "outcome", "hit").count()
}
