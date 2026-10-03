package com.dbook.application.expirebookingusecase

import com.dbook.application.ExpireBookingUseCase
import com.dbook.application.cancelbookingusecase.CancelBookingUseCaseFixture

abstract class ExpireBookingUseCaseFixture : CancelBookingUseCaseFixture() {
    protected val expireBookingUseCase = ExpireBookingUseCase(bookingRepository, useCase)
}
