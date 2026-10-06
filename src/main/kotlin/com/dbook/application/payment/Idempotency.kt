package com.dbook.application.payment

import com.dbook.domain.payment.IdempotencyKeyReusedException
import java.security.MessageDigest
import java.util.HexFormat

/** The same request always gives the same fingerprint: the SHA-256 of its parts, in order. */
fun fingerprintOf(vararg parts: Any?): String {
    val canonical = parts.joinToString("|") { it?.toString().orEmpty() }
    return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical.toByteArray()))
}

/**
 * The rules every idempotency key follows, kept in one place (payments and refunds use them): a key never seen runs
 * [perform]; the same key with the same request returns what it produced before, without running it again
 * ([onReplay] notes the replay); the same key with a different request is a client error, never a silent replay.
 */
fun <T> idempotently(
    previous: T?,
    previousFingerprint: (T) -> String?,
    fingerprint: String,
    onReplay: () -> Unit = {},
    perform: () -> T,
): T {
    if (previous == null) {
        return perform()
    }
    if (previousFingerprint(previous) != fingerprint) {
        throw IdempotencyKeyReusedException()
    }
    onReplay()
    return previous
}
