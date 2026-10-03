package com.dbook.application.booking.expirebookingusecase

import com.dbook.application.booking.ExpireBookingUseCase
import com.dbook.application.booking.cancelbookingusecase.CancelBookingUseCaseFixture
import io.micrometer.core.instrument.simple.SimpleMeterRegistry

abstract class ExpireBookingUseCaseFixture : CancelBookingUseCaseFixture() {
    protected val meterRegistry = SimpleMeterRegistry()
    protected val expireBookingUseCase = ExpireBookingUseCase(bookingRepository, useCase, meterRegistry)

    /** How many times the counter [name] was incremented for [outcome] (0 if it never was). */
    protected fun counted(
        name: String,
        outcome: String,
    ): Double = meterRegistry.find(name).tag("outcome", outcome).counter()?.count() ?: 0.0
}
