package com.dbook.infrastructure.web.requestloggingfilter

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.dbook.infrastructure.observability.ObservabilityFixture
import com.dbook.infrastructure.web.RequestLoggingFilter
import org.slf4j.LoggerFactory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

// The real application on a real port, with an in-memory appender listening to the filter's
// logger, so a test can look at the access lines (and the MDC they were written with).
abstract class RequestLoggingFixture : ObservabilityFixture() {
    private val appender = ListAppender<ILoggingEvent>()
    private val filterLogger = LoggerFactory.getLogger(RequestLoggingFilter::class.java) as Logger

    @BeforeTest
    fun startListening() {
        appender.start()
        filterLogger.addAppender(appender)
    }

    @AfterTest
    fun stopListening() {
        filterLogger.detachAppender(appender)
        appender.stop()
    }

    /** The access lines logged for [path], in order. */
    protected fun accessLinesFor(path: String): List<ILoggingEvent> =
        appender.list.toList().filter { it.formattedMessage.contains(" $path -> ") }
}
