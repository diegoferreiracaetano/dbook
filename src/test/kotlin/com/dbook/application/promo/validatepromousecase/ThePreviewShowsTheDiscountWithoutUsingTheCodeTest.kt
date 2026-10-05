package com.dbook.application.promo.validatepromousecase

import com.dbook.application.payment.registerpaymentusecase.RegisterPaymentUseCaseFixture
import com.dbook.application.promo.ValidatePromoCommand
import com.dbook.application.promo.ValidatePromoUseCase
import com.dbook.domain.booking.NotBookingOwnerException
import com.dbook.domain.promo.PromoNotFoundException
import com.dbook.domain.promo.PromoRejectedException
import com.dbook.domain.promo.PromoRejection
import java.math.BigDecimal
import java.time.Clock
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ThePreviewShowsTheDiscountWithoutUsingTheCodeTest : RegisterPaymentUseCaseFixture() {
    private val preview = ValidatePromoUseCase(promos, bookingRepository, Clock.fixed(now, ZoneOffset.UTC))
    private val both = listOf(outboundBookingId, returnBookingId)

    @Test
    fun `given two pending bookings when a code is previewed then discount and total show and nothing is used`() {
        val result = preview.execute(ValidatePromoCommand(ownerId, "welcome10", both))

        assertEquals(BigDecimal("800.00"), result.subtotal)
        assertEquals(BigDecimal("80.00"), result.discount)
        assertEquals(BigDecimal("720.00"), result.total)
        assertEquals(emptyList(), promos.redemptions)
        assertEquals(0, promos.findByCode("WELCOME10")!!.redeemed)
    }

    @Test
    fun `given a code that cannot be used when previewed then it says why`() {
        val expired =
            assertFailsWith<PromoRejectedException> { preview.execute(ValidatePromoCommand(ownerId, "EXPIRED", both)) }
        val none =
            assertFailsWith<PromoNotFoundException> { preview.execute(ValidatePromoCommand(ownerId, "NOPE", both)) }

        assertEquals(PromoRejection.EXPIRED, expired.reason)
        assertEquals("Promo code not found: NOPE", none.message)
    }

    @Test
    fun `given a code already used up or used by this customer when previewed then it is rejected`() {
        promos.redeem(3, 99, 1, BigDecimal("5.00"), now)
        promos.redeem(1, ownerId, 2, BigDecimal("5.00"), now)

        val exhausted =
            assertFailsWith<PromoRejectedException> { preview.execute(ValidatePromoCommand(ownerId, "ONCE", both)) }
        val limit =
            assertFailsWith<PromoRejectedException> {
                preview.execute(
                    ValidatePromoCommand(ownerId, "WELCOME10", both),
                )
            }

        assertEquals(PromoRejection.EXHAUSTED, exhausted.reason)
        assertEquals(PromoRejection.USER_LIMIT_REACHED, limit.reason)
    }

    @Test
    fun `given someone else's bookings or none or a paid one when previewed then it is refused`() {
        assertFailsWith<NotBookingOwnerException> { preview.execute(ValidatePromoCommand(999, "WELCOME10", both)) }
        assertFailsWith<IllegalArgumentException> {
            preview.execute(
                ValidatePromoCommand(ownerId, "WELCOME10", emptyList()),
            )
        }
        executeCommitted(
            com.dbook.application.payment.RegisterPaymentCommand(
                listOf(outboundBookingId),
                "4242",
                "Jane",
                ownerId,
                "k",
            ),
        )
        assertFailsWith<IllegalStateException> {
            preview.execute(ValidatePromoCommand(ownerId, "WELCOME10", listOf(outboundBookingId)))
        }
    }
}
