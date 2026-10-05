package com.dbook.application.identity

import com.dbook.domain.identity.Role
import com.dbook.domain.identity.TwoFactorRepository
import com.dbook.domain.identity.User
import org.springframework.stereotype.Service

/** `admin.two-factor.required-roles`: the roles that cannot sign in to the portal without a second factor. */
data class TwoFactorPolicy(
    val requiredRoles: Set<Role>,
)

/** Who has a second factor and who must have one: the questions the sign-in asks before it issues a session. */
@Service
class TwoFactorGate(
    private val repository: TwoFactorRepository,
    private val policy: TwoFactorPolicy,
) {
    fun isEnabled(userId: Long): Boolean = repository.find(userId)?.isActive == true

    fun isRequiredFor(role: Role): Boolean = role in policy.requiredRoles

    /** A staff account with a second factor, or one that must have it, does not sign in with the password alone. */
    fun guards(user: User): Boolean =
        user.role.isStaff && (isRequiredFor(user.role) || isEnabled(requireNotNull(user.id)))
}
