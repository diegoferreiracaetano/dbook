package com.dbook.application.identity

/** Where the links mailed to a customer point: the customer app, which serves these two routes. */
data class CustomerAppLinks(
    val baseUrl: String,
) {
    fun verifyEmail(token: String) = "$baseUrl/verify-email?token=$token"

    fun resetPassword(token: String) = "$baseUrl/reset-password?token=$token"
}
