package com.dbook.domain.identity

import java.time.Instant

class User(
    val id: Long? = null,
    val email: String,
    val passwordHash: String,
    val name: String,
    val role: Role = Role.CLIENT,
    val status: UserStatus = UserStatus.ACTIVE,
    val blockedReason: String? = null,
    val blockedAt: Instant? = null,
    val lastLoginAt: Instant? = null,
    val anonymizedAt: Instant? = null,
    val version: Long = 0,
) {
    init {
        require(email.isNotBlank() && email.contains("@")) { "email must be a valid address" }
        require(passwordHash.isNotBlank()) { "passwordHash must not be blank" }
        require(name.isNotBlank()) { "name must not be blank" }
        when (status) {
            UserStatus.BLOCKED ->
                require(!blockedReason.isNullOrBlank() && blockedAt != null) {
                    "a blocked user needs the reason and the moment it was blocked"
                }
            UserStatus.ACTIVE ->
                require(blockedReason == null && blockedAt == null) { "an active user has no block details" }
        }
    }

    val isBlocked: Boolean get() = status == UserStatus.BLOCKED

    val isAnonymized: Boolean get() = anonymizedAt != null

    fun block(
        reason: String,
        at: Instant,
    ): User {
        check(!isBlocked) { "User is already blocked" }
        require(reason.isNotBlank()) { "reason must not be blank" }
        return withBlockState(UserStatus.BLOCKED, reason.trim(), at)
    }

    fun unblock(): User {
        check(isBlocked) { "User is not blocked" }
        check(!isAnonymized) { "An anonymized account cannot be reopened" }
        return withBlockState(UserStatus.ACTIVE, null, null)
    }

    fun rename(newName: String): User = withProfile(name = newName)

    fun changeRole(newRole: Role): User = withProfile(role = newRole)

    fun withPasswordHash(newHash: String): User = withProfile(passwordHash = newHash)

    // Irreversible. The account is left blocked with a password hash nothing can match, and the address becomes one
    // that cannot receive mail (".invalid" is reserved), so what remains identifies nobody.
    fun anonymize(now: Instant): User {
        check(role == Role.CLIENT) { "Only a customer account can be anonymized" }
        check(!isAnonymized) { "The account is already anonymized" }
        val userId = requireNotNull(id) { "an unsaved user cannot be anonymized" }
        return User(
            id = userId,
            email = "anonymized-$userId@$ANONYMIZED_DOMAIN",
            passwordHash = UNUSABLE_PASSWORD_HASH,
            name = ANONYMIZED_NAME,
            role = role,
            status = UserStatus.BLOCKED,
            blockedReason = ANONYMIZED_REASON,
            blockedAt = now,
            lastLoginAt = null,
            anonymizedAt = now,
            version = version,
        )
    }

    // every change goes through one of these two so no field is dropped (rebuilding a User by hand would undo a block)
    private fun withProfile(
        name: String = this.name,
        role: Role = this.role,
        passwordHash: String = this.passwordHash,
    ) = User(id, email, passwordHash, name, role, status, blockedReason, blockedAt, lastLoginAt, anonymizedAt, version)

    private fun withBlockState(
        status: UserStatus,
        reason: String?,
        at: Instant?,
    ) = User(id, email, passwordHash, name, role, status, reason, at, lastLoginAt, anonymizedAt, version)

    companion object {
        const val ANONYMIZED_NAME = "Anonymous customer"
        const val ANONYMIZED_DOMAIN = "anonymous.invalid"
        private const val ANONYMIZED_REASON = "Account anonymized"

        // not a BCrypt hash: the password encoder refuses to match it, whatever is typed
        private const val UNUSABLE_PASSWORD_HASH = "!anonymized"
    }
}
