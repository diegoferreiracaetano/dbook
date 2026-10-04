package com.dbook.application.identity

import com.dbook.domain.identity.User
import com.dbook.domain.identity.UserRepository
import io.micrometer.observation.annotation.Observed
import org.springframework.stereotype.Service

@Observed(name = "dbook.usecase")
@Service
class ListStaffUseCase(
    private val userRepository: UserRepository,
) {
    fun execute(): List<User> = userRepository.findStaff()
}
