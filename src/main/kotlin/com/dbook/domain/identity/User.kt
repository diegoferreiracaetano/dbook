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
        return withBlockState(UserStatus.ACTIVE, null, null)
    }

    fun rename(newName: String): User = withProfile(name = newName)

    fun changeRole(newRole: Role): User = withProfile(role = newRole)

    fun withPasswordHash(newHash: String): User = withProfile(passwordHash = newHash)

    // every change goes through one of these two so no field is dropped (rebuilding a User by hand would undo a block)
    private fun withProfile(
        name: String = this.name,
        role: Role = this.role,
        passwordHash: String = this.passwordHash,
    ) = User(id, email, passwordHash, name, role, status, blockedReason, blockedAt, lastLoginAt, version)

    private fun withBlockState(
        status: UserStatus,
        reason: String?,
        at: Instant?,
    ) = User(id, email, passwordHash, name, role, status, reason, at, lastLoginAt, version)
}
