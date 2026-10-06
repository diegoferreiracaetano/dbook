package com.dbook.application.identity

import com.dbook.domain.common.access.Actor
import com.dbook.domain.common.access.Role
import com.dbook.domain.identity.User
import com.dbook.domain.identity.UserNotFoundException
import com.dbook.domain.identity.UserRepository
import org.springframework.stereotype.Service

// The rules that keep the team from locking itself out.
@Service
class StaffSafeguards(
    private val userRepository: UserRepository,
) {
    // a customer's id looks exactly like an unknown id: the staff endpoints never reveal who is a customer
    fun staffMember(id: Long): User =
        userRepository.findById(id)?.takeIf { it.role.isStaff } ?: throw UserNotFoundException(id)

    fun requireNotSelf(
        actor: Actor,
        targetId: Long,
    ) {
        check(actor.id != targetId) { "You cannot change your own account" }
    }

    // Locks every active SUPER_ADMIN first, then counts: two requests that would each leave the other as the
    // only one queue up, and the second sees the first one's result. Must run inside the caller's transaction.
    fun requireSuperAdminRemainsWithout(target: User) {
        if (target.role != Role.SUPER_ADMIN || target.isBlocked) {
            return
        }
        val others = userRepository.lockActiveByRole(Role.SUPER_ADMIN).filter { it.id != target.id }
        check(others.isNotEmpty()) { "The system must keep at least one active SUPER_ADMIN" }
    }
}
