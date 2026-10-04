package com.dbook.presentation.identity

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Component
import java.time.Duration

// httpOnly keeps scripts from reading the refresh token; SameSite=Strict plus the Origin check keep other sites
// from making the browser send it; the path keeps it off ordinary API calls.
@Component
class AdminSessionCookie(
    @Value("\${admin-portal.cookie-secure}") private val secure: Boolean,
    @Value("\${jwt.refresh-token-expiration-days}") private val refreshTokenDays: Long,
) {
    fun issue(refreshToken: String): ResponseCookie =
        base(refreshToken).maxAge(Duration.ofDays(refreshTokenDays)).build()

    fun clear(): ResponseCookie = base("").maxAge(Duration.ZERO).build()

    private fun base(value: String) =
        ResponseCookie.from(NAME, value).httpOnly(true).secure(secure).sameSite("Strict").path(PATH)

    companion object {
        const val NAME = "dbook_admin_refresh"
        const val PATH = "/v1/admin/auth"
    }
}
