package com.dbook.presentation

import com.dbook.domain.Role
import org.springframework.security.core.Authentication

/** The authenticated user's id — `JwtAuthenticationFilter` sets the principal name to it. */
fun Authentication.currentUserId(): Long = name.toLong()

/** The authenticated user's [Role] — `JwtAuthenticationFilter` sets a single `ROLE_*` authority. */
fun Authentication.currentRole(): Role = authorities.first().authority.removePrefix("ROLE_").let(Role::valueOf)
