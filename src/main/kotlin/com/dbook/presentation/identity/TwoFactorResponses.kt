package com.dbook.presentation.identity

import com.dbook.application.identity.TotpEnrollmentStart
import io.swagger.v3.oas.annotations.media.Schema

/** The password was right but the session is not open yet: what to do next. */
data class TwoFactorChallengeResponse(
    @get:Schema(description = "Goes back, with the code, to /admin/auth/2fa/verify (or the enrollment endpoints)")
    val challengeToken: String,
    @get:Schema(description = "True when the account has no second factor yet and its role requires one")
    val enrollmentRequired: Boolean,
)

data class TwoFactorEnrollmentResponse(
    @get:Schema(description = "For the QR code the authenticator app reads")
    val otpauthUri: String,
    @get:Schema(description = "The same secret, to type into the authenticator by hand")
    val manualEntryKey: String,
) {
    companion object {
        fun from(start: TotpEnrollmentStart) = TwoFactorEnrollmentResponse(start.otpauthUri, start.manualEntryKey)
    }
}

data class RecoveryCodesResponse(
    @get:Schema(description = "Shown this once: only their hashes are kept")
    val recoveryCodes: List<String>,
)

/** The session of a sign-in that enrolled the second factor on the way. */
data class AdminEnrolledSessionResponse(
    val accessToken: String,
    val recoveryCodes: List<String>,
)
