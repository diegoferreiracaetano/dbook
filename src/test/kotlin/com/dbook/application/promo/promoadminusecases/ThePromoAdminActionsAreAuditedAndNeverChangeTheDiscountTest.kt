package com.dbook.application.promo.promoadminusecases

import com.dbook.application.audit.FakeAuditLog
import com.dbook.application.promo.CreatePromoCommand
import com.dbook.application.promo.CreatePromoUseCase
import com.dbook.application.promo.InMemoryPromos
import com.dbook.application.promo.SetPromoActiveUseCase
import com.dbook.application.promo.UpdatePromoCommand
import com.dbook.application.promo.UpdatePromoUseCase
import com.dbook.domain.common.access.Actor
import com.dbook.domain.common.access.Role
import com.dbook.domain.common.audit.AuditAction
import com.dbook.domain.promo.DuplicatePromoCodeException
import com.dbook.domain.promo.PromoType
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ThePromoAdminActionsAreAuditedAndNeverChangeTheDiscountTest {
    private val now: Instant = Instant.parse("2026-10-04T12:00:00Z")
    private val actor = Actor(5, Role.CATALOG_MANAGER)
    private val promos = InMemoryPromos()
    private val audit = FakeAuditLog()
    private val create = CreatePromoUseCase(promos, audit, Clock.fixed(now, ZoneOffset.UTC))
    private val update = UpdatePromoUseCase(promos, audit)
    private val setActive = SetPromoActiveUseCase(promos, audit)

    private fun created() =
        create.execute(
            CreatePromoCommand(
                actor, " summer20 ", PromoType.PERCENT, BigDecimal("20"), BigDecimal.ZERO, now, now.plusSeconds(3_600),
                maxRedemptions = 100, maxPerUser = 1,
            ),
        )

    @Test
    fun `given a lower-case code when created then it is kept in capitals and audited, and a repeat is a conflict`() {
        val promo = created()

        assertEquals("SUMMER20", promo.code)
        assertEquals(AuditAction.PROMO_CREATED, audit.events.single().action)
        assertEquals("SUMMER20", audit.events.single().after?.get("code"))
        assertFailsWith<DuplicatePromoCodeException> { created() }
    }

    @Test
    fun `given a code when its window and limits change then code, type and value stay and both states are audited`() {
        val promo = created()

        val changed =
            update.execute(
                UpdatePromoCommand(actor, promo.id!!, BigDecimal("50"), now, now.plusSeconds(7_200), 200, 2),
            )

        assertEquals(BigDecimal("20"), changed.value)
        assertEquals(PromoType.PERCENT, changed.type)
        assertEquals(200, changed.maxRedemptions)
        assertEquals(AuditAction.PROMO_UPDATED, audit.events.last().action)
        assertEquals(100, audit.events.last().before?.get("maxRedemptions"))
        assertEquals(200, audit.events.last().after?.get("maxRedemptions"))
    }

    @Test
    fun `given a used code when its limit is lowered below the uses then it is refused`() {
        val promo = created()
        promos.redeem(promo.id!!, 1, 1, BigDecimal("5"), now)
        promos.redeem(promo.id!!, 2, 2, BigDecimal("5"), now)

        assertFailsWith<IllegalArgumentException> {
            update.execute(UpdatePromoCommand(actor, promo.id!!, BigDecimal.ZERO, now, now.plusSeconds(3_600), 1, 1))
        }
    }

    @Test
    fun `given a code when deactivated and activated then each is audited, and repeating either is a conflict`() {
        val promo = created()

        assertEquals(false, setActive.execute(actor, promo.id!!, active = false).active)
        assertFailsWith<IllegalStateException> { setActive.execute(actor, promo.id!!, active = false) }
        assertEquals(true, setActive.execute(actor, promo.id!!, active = true).active)

        assertEquals(
            listOf(AuditAction.PROMO_CREATED, AuditAction.PROMO_DEACTIVATED, AuditAction.PROMO_ACTIVATED),
            audit.events.map { it.action },
        )
    }
}
