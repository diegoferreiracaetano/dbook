package com.dbook.presentation.booking.bookingexpirationconsumer

import com.dbook.LocalStackSqs
import com.dbook.application.booking.expirebookingusecase.ExpireBookingUseCaseFixture
import com.dbook.presentation.booking.BookingExpirationConsumer
import com.fasterxml.jackson.databind.ObjectMapper
import io.micrometer.tracing.Tracer
import io.micrometer.tracing.propagation.Propagator

abstract class BookingExpirationConsumerFixture : ExpireBookingUseCaseFixture() {
    protected fun consumerFor(
        queueUrl: String,
        tracer: Tracer = Tracer.NOOP,
        propagator: Propagator = Propagator.NOOP,
    ) = BookingExpirationConsumer(
        LocalStackSqs.client,
        expireBookingUseCase,
        ObjectMapper(),
        meterRegistry,
        tracer,
        propagator,
        queueUrl,
        waitSeconds = 1,
    )

    // longer than the 1 s visibility timeout the queues below use: a message that was NOT
    // deleted would be visible again by now
    protected fun waitForRedelivery() = Thread.sleep(REDELIVERY_WAIT_MS)

    private companion object {
        const val REDELIVERY_WAIT_MS = 2_000L
    }
}
