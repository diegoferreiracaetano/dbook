package com.dbook.application.promo

import com.dbook.domain.audit.AuditAction
import com.dbook.domain.audit.AuditEvent
import com.dbook.domain.audit.AuditLog
import com.dbook.domain.common.PageQuery
import com.dbook.domain.common.PageResult
import com.dbook.domain.identity.Actor
import com.dbook.domain.promo.AdminPromoReader
import com.dbook.domain.promo.PromoCode
import com.dbook.domain.promo.PromoNotFoundException
import com.dbook.domain.promo.PromoRedemptionRow
import com.dbook.domain.promo.PromoRepository
import com.dbook.domain.promo.PromoType
import com.dbook.domain.promo.toAuditSnapshot
import io.micrometer.observation.annotation.Observed
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant

data class CreatePromoCommand(
    val actor: Actor,
    val code: String,
    val type: PromoType,
    val value: BigDecimal,
    val minAmount: BigDecimal,
    val validFrom: Instant,
    val validUntil: Instant,
    val maxRedemptions: Int?,
    val maxPerUser: Int,
)

/** Creates a code. A code that exists (in any case) is a 409. Audited. */
@Observed(name = "dbook.usecase")
@Service
class CreatePromoUseCase(
    private val promoRepository: PromoRepository,
    private val auditLog: AuditLog,
    private val clock: Clock,
) {
    @Transactional
    fun execute(command: CreatePromoCommand): PromoCode {
        val promo =
            promoRepository.save(
                PromoCode(
                    code = PromoCode.normalize(command.code),
                    type = command.type,
                    value = command.value,
                    minAmount = command.minAmount,
                    validFrom = command.validFrom,
                    validUntil = command.validUntil,
                    maxRedemptions = command.maxRedemptions,
                    maxPerUser = command.maxPerUser,
                    createdBy = command.actor.id,
                    createdAt = clock.instant(),
                ),
            )
        auditLog.record(
            AuditEvent(
                command.actor,
                AuditAction.PROMO_CREATED,
                requireNotNull(promo.id).toString(),
                after = promo.toAuditSnapshot(),
            ),
        )
        return promo
    }
}

data class UpdatePromoCommand(
    val actor: Actor,
    val promoId: Long,
    val minAmount: BigDecimal,
    val validFrom: Instant,
    val validUntil: Instant,
    val maxRedemptions: Int?,
    val maxPerUser: Int,
)

/** Moves the window, the minimum and the limits. The code, its type and its value are never changed. Audited. */
@Observed(name = "dbook.usecase")
@Service
class UpdatePromoUseCase(
    private val promoRepository: PromoRepository,
    private val auditLog: AuditLog,
) {
    @Transactional
    fun execute(command: UpdatePromoCommand): PromoCode {
        val before = promoRepository.findById(command.promoId) ?: throw PromoNotFoundException("#${command.promoId}")
        val changed =
            promoRepository.save(
                before.copy(
                    minAmount = command.minAmount,
                    validFrom = command.validFrom,
                    validUntil = command.validUntil,
                    maxRedemptions = command.maxRedemptions,
                    maxPerUser = command.maxPerUser,
                ),
            )
        auditLog.record(
            AuditEvent(
                command.actor,
                AuditAction.PROMO_UPDATED,
                command.promoId.toString(),
                before = before.toAuditSnapshot(),
                after = changed.toAuditSnapshot(),
            ),
        )
        return changed
    }
}

/** Switches a code off (it is never deleted: the payments that used it keep pointing at it) or back on. Audited. */
@Observed(name = "dbook.usecase")
@Service
class SetPromoActiveUseCase(
    private val promoRepository: PromoRepository,
    private val auditLog: AuditLog,
) {
    @Transactional
    fun execute(
        actor: Actor,
        promoId: Long,
        active: Boolean,
    ): PromoCode {
        val before = promoRepository.findById(promoId) ?: throw PromoNotFoundException("#$promoId")
        check(
            before.active != active,
        ) { "Promo code ${before.code} is already ${if (active) "active" else "inactive"}" }
        val changed = promoRepository.save(before.copy(active = active))
        auditLog.record(
            AuditEvent(
                actor,
                if (active) AuditAction.PROMO_ACTIVATED else AuditAction.PROMO_DEACTIVATED,
                promoId.toString(),
                before = before.toAuditSnapshot(),
                after = changed.toAuditSnapshot(),
            ),
        )
        return changed
    }
}

@Observed(name = "dbook.usecase")
@Service
class ListPromosUseCase(
    private val reader: AdminPromoReader,
    private val promoRepository: PromoRepository,
) {
    fun search(
        active: Boolean?,
        page: PageQuery,
    ): PageResult<PromoCode> = reader.search(active, page)

    fun find(id: Long): PromoCode = promoRepository.findById(id) ?: throw PromoNotFoundException("#$id")

    fun redemptions(
        id: Long,
        page: PageQuery,
    ): PageResult<PromoRedemptionRow> {
        find(id)
        return reader.redemptions(id, page)
    }
}
