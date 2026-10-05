package com.dbook.infrastructure.persistence

import com.dbook.AbstractIntegrationTest
import com.zaxxer.hikari.HikariDataSource
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.beans.factory.annotation.Autowired
import javax.sql.DataSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TheConnectionPoolIsSizedAndObservedTest : AbstractIntegrationTest() {
    @Autowired
    lateinit var dataSource: DataSource

    @Autowired
    lateinit var meters: MeterRegistry

    @Test
    fun `given the app is running when the pool is read then it is the sized one and its gauges are exported`() {
        val pool = dataSource.unwrap(HikariDataSource::class.java)

        assertEquals("dbook-pool", pool.poolName)
        assertEquals(10, pool.maximumPoolSize)
        assertEquals(3_000, pool.connectionTimeout)
        assertEquals(20_000, pool.leakDetectionThreshold)
        val max = meters.find("hikaricp.connections.max").tag("pool", "dbook-pool").gauge()
        assertNotNull(meters.find("hikaricp.connections.pending").tag("pool", "dbook-pool").gauge())
        assertEquals(10.0, max?.value())
    }
}
