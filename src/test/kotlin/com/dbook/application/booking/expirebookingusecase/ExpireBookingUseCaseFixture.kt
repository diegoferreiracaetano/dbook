package com.dbook.application.booking.expirebookingusecase

import com.dbook.application.booking.ExpireBookingUseCase
import com.dbook.application.booking.cancelbookingusecase.CancelBookingUseCaseFixture

abstract class ExpireBookingUseCaseFixture : CancelBookingUseCaseFixture() {
    protected val expireBookingUseCase = ExpireBookingUseCase(bookingRepository, useCase)
}
