package com.dbook.application.identity

import com.dbook.domain.identity.Role

enum class SessionAudience {
    CLIENT,
    STAFF,
    ;

    // a client's credentials are refused at the portal exactly like a wrong password
    fun accepts(role: Role): Boolean = this == CLIENT || role.isStaff

    val tag: String get() = name.lowercase()
}
