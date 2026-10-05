package com.dbook.application.identity

import com.dbook.domain.audit.AuditAction
import com.dbook.domain.audit.AuditEvent
import com.dbook.domain.audit.AuditLog
import com.dbook.domain.identity.Actor
import com.dbook.domain.identity.InvalidTwoFactorCodeException
import com.dbook.domain.identity.RecoveryCodeGenerator
import com.dbook.domain.identity.SecretCipher
import com.dbook.domain.identity.TokenService
import com.dbook.domain.identity.TotpService
import com.dbook.domain.identity.TwoFactorRepository
import com.dbook.domain.identity.UserNotFoundException
import com.dbook.domain.identity.UserRepository
import io.micrometer.observation.annotation.Observed
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

/** What the authenticator needs: the URI for the QR code, and the same secret to type in by hand. */
data class TotpEnrollmentStart(
    val otpauthUri: String,
    val manualEntryKey: String,
)

/**
 * Starts the enrollment of a second factor: a new secret (encrypted at rest) that does not protect anything until
 * the first code from the authenticator confirms it. Starting again before confirming simply replaces the secret.
 */
@Observed(name = "dbook.usecase")
@Service
class EnrollTotpUseCase(
    private val users: UserRepository,
    private val repository: TwoFactorRepository,
    private val totp: TotpService,
    private val cipher: SecretCipher,
) {
    @Transactional
    fun execute(userId: Long): TotpEnrollmentStart {
        val user = users.findById(userId) ?: throw UserNotFoundException(userId)
        check(
            repository.find(userId)?.isActive != true,
        ) { "The second factor is already on; turn it off to enroll again" }
        val secret = totp.newSecret()
        repository.savePending(userId, cipher.encrypt(secret))
        return TotpEnrollmentStart(totp.otpauthUri(secret, user.email), totp.manualEntryKey(secret))
    }
}

/**
 * Finishes the enrollment with the first code. It turns the second factor on and makes the recovery codes, which are
 * returned **this once** (only their hashes are kept). A wrong code leaves the enrollment pending, to try again.
 */
@Observed(name = "dbook.usecase")
@Service
class ConfirmTotpUseCase(
    private val repository: TwoFactorRepository,
    private val totp: TotpService,
    private val cipher: SecretCipher,
    private val recoveryCodes: RecoveryCodeGenerator,
    private val tokenService: TokenService,
    private val auditLog: AuditLog,
    private val clock: Clock,
) {
    @Transactional
    fun execute(
        actor: Actor,
        code: String,
    ): List<String> {
        val pending = repository.find(actor.id)?.takeIf { !it.isActive }
        checkNotNull(pending) { "Start the enrollment first" }
        val step =
            totp.matchingStep(cipher.decrypt(pending.secretEncrypted), code.trim(), clock.instant(), 0)
                ?: throw InvalidTwoFactorCodeException()
        val codes = List(RECOVERY_CODES) { recoveryCodes.generate() }
        check(repository.confirm(actor.id, step, clock.instant(), codes.map { tokenService.hashToken(it) })) {
            "The enrollment was confirmed by someone else meanwhile"
        }
        auditLog.record(AuditEvent(actor, AuditAction.TWO_FACTOR_ENABLED, actor.id.toString()))
        return codes
    }

    private companion object {
        const val RECOVERY_CODES = 10
    }
}

data class DisableTwoFactorCommand(
    val actor: Actor,
    val password: String,
    val code: String,
    val clientIp: String,
)

/**
 * Turns the second factor off. It takes the password **and** a code (a stolen session alone cannot do it), and it is
 * refused for the roles the policy requires to have one: for them the only way out is a reset by another SUPER_ADMIN.
 */
@Observed(name = "dbook.usecase")
@Service
class DisableTwoFactorUseCase(
    private val users: UserRepository,
    private val repository: TwoFactorRepository,
    private val gate: TwoFactorGate,
    private val passwordConfirmation: PasswordConfirmation,
    private val verifier: SecondFactorVerifier,
    private val auditLog: AuditLog,
) {
    @Transactional
    fun execute(command: DisableTwoFactorCommand) {
        val user = users.findById(command.actor.id) ?: throw UserNotFoundException(command.actor.id)
        check(!gate.isRequiredFor(user.role)) { "Your role must keep the second factor on" }
        check(gate.isEnabled(command.actor.id)) { "The second factor is not on" }
        passwordConfirmation.confirm(user, command.password, command.clientIp)
        if (!verifier.accepts(command.actor.id, command.code)) {
            throw InvalidTwoFactorCodeException()
        }
        repository.remove(command.actor.id)
        auditLog.record(AuditEvent(command.actor, AuditAction.TWO_FACTOR_DISABLED, command.actor.id.toString()))
    }
}

data class ResetTwoFactorCommand(
    val actor: Actor,
    val targetId: Long,
    val reason: String,
)

/**
 * The way out for a staff member who lost the authenticator **and** the recovery codes: another SUPER_ADMIN removes
 * their second factor, with a reason, on the record. Their sessions end, and they enroll again at the next sign-in
 * (or at once, where the role requires it). Nobody resets their own: that would be the door the second factor closes.
 */
@Observed(name = "dbook.usecase")
@Service
class ResetTwoFactorUseCase(
    private val safeguards: StaffSafeguards,
    private val repository: TwoFactorRepository,
    private val refreshTokens: com.dbook.domain.identity.RefreshTokenRepository,
    private val auditLog: AuditLog,
) {
    @Transactional
    fun execute(command: ResetTwoFactorCommand) {
        require(command.reason.trim().length >= MIN_REASON) { "reason must have at least $MIN_REASON characters" }
        safeguards.requireNotSelf(command.actor, command.targetId)
        safeguards.staffMember(command.targetId)
        checkNotNull(repository.find(command.targetId)) { "This account has no second factor to reset" }
        repository.remove(command.targetId)
        refreshTokens.revokeAllForUser(command.targetId)
        auditLog.record(
            AuditEvent(
                actor = command.actor,
                action = AuditAction.TWO_FACTOR_RESET,
                targetId = command.targetId.toString(),
                reason = command.reason.trim(),
            ),
        )
    }

    private companion object {
        const val MIN_REASON = 10
    }
}
