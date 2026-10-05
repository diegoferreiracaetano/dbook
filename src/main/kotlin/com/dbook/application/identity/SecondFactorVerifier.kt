package com.dbook.application.identity

import com.dbook.domain.identity.SecretCipher
import com.dbook.domain.identity.TokenService
import com.dbook.domain.identity.TotpEnrollment
import com.dbook.domain.identity.TotpService
import com.dbook.domain.identity.TwoFactorRepository
import org.springframework.stereotype.Service
import java.time.Clock

/**
 * Is this the account's second factor? A six-digit code from the authenticator, or a recovery code. Both are good
 * **once**: the authenticator's by the time step it was made for, the recovery code by being spent. The answer is
 * the decision of a conditional UPDATE, so two requests with the same code cannot both pass.
 */
@Service
class SecondFactorVerifier(
    private val repository: TwoFactorRepository,
    private val totp: TotpService,
    private val cipher: SecretCipher,
    private val tokenService: TokenService,
    private val clock: Clock,
) {
    fun accepts(
        userId: Long,
        code: String,
    ): Boolean {
        val enrollment = repository.find(userId)?.takeIf { it.isActive } ?: return false
        val trimmed = code.trim()
        return if (AUTHENTICATOR_CODE.matches(trimmed)) {
            authenticatorCodeAccepted(enrollment, trimmed)
        } else {
            repository.spendRecoveryCode(userId, tokenService.hashToken(normalizedRecovery(trimmed)), clock.instant())
        }
    }

    private fun authenticatorCodeAccepted(
        enrollment: TotpEnrollment,
        code: String,
    ): Boolean {
        val secret = cipher.decrypt(enrollment.secretEncrypted)
        val step = totp.matchingStep(secret, code, clock.instant(), enrollment.lastUsedStep)
        return step != null && repository.advanceStep(enrollment.userId, step)
    }

    companion object {
        private val AUTHENTICATOR_CODE = Regex("^\\d{6}$")

        /** Recovery codes are typed by people: case and spaces do not matter. */
        fun normalizedRecovery(code: String): String = code.trim().uppercase().replace(" ", "")
    }
}
