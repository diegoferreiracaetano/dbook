package com.dbook.application.crm

import com.dbook.domain.common.access.Role
import com.dbook.domain.identity.User
import com.dbook.domain.identity.UserNotFoundException
import com.dbook.domain.identity.UserRepository
import org.springframework.stereotype.Service

// The CRM only deals with customers: a staff member's id looks exactly like an unknown one.
@Service
class CustomerGuard(
    private val userRepository: UserRepository,
) {
    fun customer(id: Long): User =
        userRepository.findById(id)?.takeIf { it.role == Role.CLIENT } ?: throw UserNotFoundException(id)
}
