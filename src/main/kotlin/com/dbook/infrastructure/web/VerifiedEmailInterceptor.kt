package com.dbook.infrastructure.web

import com.dbook.domain.identity.EmailNotVerifiedException
import com.dbook.domain.identity.UserRepository
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

/**
 * Booking and paying (the POSTs it is registered for) ask for a confirmed e-mail address when
 * `account.require-verified-email` is on: the receipts, the refunds and the flight changes are mailed there. Reading,
 * searching and favouriting stay open to a new account. Asked of the database each time (a confirmation takes effect at
 * once, with no new sign-in), on the few writes that matter.
 */
@Component
class VerifiedEmailInterceptor(
    private val users: UserRepository,
    @Value("\${account.require-verified-email:false}") private val required: Boolean,
) : HandlerInterceptor {
    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        val authentication = SecurityContextHolder.getContext().authentication
        val userId =
            authentication?.takeIf { it.isAuthenticated && it !is AnonymousAuthenticationToken }?.name?.toLongOrNull()
        if (required && request.method == HttpMethod.POST.name() && userId != null) {
            if (users.findById(userId)?.isEmailVerified == false) {
                throw EmailNotVerifiedException()
            }
        }
        return true
    }
}
