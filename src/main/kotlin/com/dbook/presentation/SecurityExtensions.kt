package com.dbook.presentation

import com.dbook.domain.Role
import org.springframework.security.core.Authentication

// JwtAuthenticationFilter sets the principal to the user id (as String) and a single
// ROLE_* authority — these extensions turn that back into domain types for controllers.
fun Authentication.currentUserId(): Long = name.toLong()

fun Authentication.currentRole(): Role = authorities.first().authority.removePrefix("ROLE_").let(Role::valueOf)
