package com.dbook.presentation.lifecycle

import com.dbook.AbstractIntegrationTest
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc

@AutoConfigureMockMvc
abstract class LifecycleFixture : AbstractIntegrationTest() {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var meterRegistry: MeterRegistry

    protected fun count(
        name: String,
        vararg tags: String,
    ): Double = meterRegistry.find(name).tags(*tags).counter()?.count() ?: 0.0
}
