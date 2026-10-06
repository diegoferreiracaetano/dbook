package com.dbook.domain.promo

fun PromoCode.toAuditSnapshot(): Map<String, Any?> =
    mapOf(
        "id" to id, "code" to code, "type" to type.name, "value" to value.toPlainString(),
        "minAmount" to minAmount.toPlainString(), "validFrom" to validFrom.toString(),
        "validUntil" to validUntil.toString(), "maxRedemptions" to maxRedemptions, "maxPerUser" to maxPerUser,
        "redeemed" to redeemed, "active" to active,
    )
