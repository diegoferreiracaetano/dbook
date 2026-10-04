package com.dbook.infrastructure.security

import com.dbook.domain.identity.Role
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority

fun Role.toAuthorities(): List<GrantedAuthority> =
    listOf(SimpleGrantedAuthority("ROLE_$name")) + permissions.map { SimpleGrantedAuthority(it.name) }
