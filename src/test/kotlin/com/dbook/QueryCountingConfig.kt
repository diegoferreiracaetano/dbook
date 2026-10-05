package com.dbook

import io.micrometer.observation.Observation
import io.micrometer.observation.ObservationHandler
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

// Every SQL statement the application runs is observed as "jdbc.query" (datasource-micrometer). This handler
// counts them per thread, so a test can assert how many queries an operation costs and catch an N+1 early.
class QueryCounter : ObservationHandler<Observation.Context> {
    private val count = ThreadLocal.withInitial { 0 }

    override fun supportsContext(context: Observation.Context) = context.name == "jdbc.query"

    override fun onStart(context: Observation.Context) = count.set(count.get() + 1)

    /** The number of SQL statements [action] ran on the calling thread. */
    fun <T> queriesOf(action: () -> T): Int {
        count.set(0)
        action()
        return count.get()
    }
}

@TestConfiguration
class QueryCountingConfig {
    @Bean
    fun queryCounter() = QueryCounter()
}
