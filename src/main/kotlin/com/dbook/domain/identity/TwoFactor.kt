package com.dbook.domain.identity

import java.time.Instant

/**
 * Time-based one-time passwords (RFC 6238), the second factor of the team's sign-in. The domain only knows what it
 * needs to ask; the HMAC and the clock arithmetic live in the adapter.
 */
interface TotpService {
    /** A fresh random shared secret. */
    fun newSecret(): ByteArray

    /**
     * The time step of [code] when it is right now (or one step either side, for clocks that drift) **and newer than
     * [afterStep]**, otherwise null. A code is good once: the step it matched is recorded by the caller, and the same
     * code (or any older one) is refused from then on.
     */
    fun matchingStep(
        secret: ByteArray,
        code: String,
        now: Instant,
        afterStep: Long,
    ): Long?

    /** The `otpauth://` URI an authenticator app reads from a QR code. */
    fun otpauthUri(
        secret: ByteArray,
        account: String,
    ): String

    /** The secret as the authenticator shows it for typing it in by hand (Base32). */
    fun manualEntryKey(secret: ByteArray): String
}

/** Keeps a TOTP secret unreadable at rest: what is stored in the database is never the secret itself. */
interface SecretCipher {
    fun encrypt(plain: ByteArray): String

    fun decrypt(stored: String): ByteArray
}

/** A one-time code for when the authenticator is lost: shown once at enrollment, stored only as a hash. */
interface RecoveryCodeGenerator {
    fun generate(): String
}

/** The second factor of a staff member. [confirmedAt] null: the enrollment was started and not finished. */
data class TotpEnrollment(
    val userId: Long,
    val secretEncrypted: String,
    val confirmedAt: Instant?,
    val lastUsedStep: Long,
) {
    val isActive: Boolean get() = confirmedAt != null
}

interface TwoFactorRepository {
    fun find(userId: Long): TotpEnrollment?

    /** Starts (or restarts) an enrollment with a new secret. Never touches one that was already confirmed. */
    fun savePending(
        userId: Long,
        secretEncrypted: String,
    )

    /** Activates the pending enrollment and stores its recovery codes, in one step. False if there is none pending. */
    fun confirm(
        userId: Long,
        step: Long,
        now: Instant,
        recoveryCodeHashes: List<String>,
    ): Boolean

    /** Records that [step] was used, only if it is newer than the last one: the answer is the decision. */
    fun advanceStep(
        userId: Long,
        step: Long,
    ): Boolean

    /** Spends the recovery code with this hash, if it is there and unspent: the answer is the decision. */
    fun spendRecoveryCode(
        userId: Long,
        hash: String,
        now: Instant,
    ): Boolean

    fun unspentRecoveryCodes(userId: Long): Int

    /** Removes the second factor and its recovery codes. */
    fun remove(userId: Long)
}

class InvalidTwoFactorCodeException : RuntimeException("The code is not valid")

/** A staff account that must (or does) use a second factor cannot sign in through the client endpoint. */
class TwoFactorRequiredException :
    RuntimeException("This account signs in through the admin portal, with a second factor")

enum class ChallengePurpose {
    /** The password was right and the account has a second factor: the next step is the code. */
    VERIFY,

    /** The password was right and the account must have a second factor but has none yet: enroll one. */
    ENROLL,
}

/** What a challenge token says: who passed the password, and what they must do next. */
data class TwoFactorChallenge(
    val userId: Long,
    val purpose: ChallengePurpose,
)
