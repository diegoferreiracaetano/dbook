package com.dbook.presentation.common

import com.dbook.domain.identity.Actor
import com.dbook.domain.identity.Role
import org.springframework.security.core.Authentication

/** The authenticated user's id — `JwtAuthenticationFilter` sets the principal name to it. */
fun Authentication.currentUserId(): Long = name.toLong()

/** The authenticated user's [Role] — the `ROLE_*` authority, which comes before the permission authorities. */
fun Authentication.currentRole(): Role =
    authorities.first { it.authority.startsWith("ROLE_") }.authority.removePrefix("ROLE_").let(Role::valueOf)

fun Authentication.currentActor(): Actor = Actor(currentUserId(), currentRole())
