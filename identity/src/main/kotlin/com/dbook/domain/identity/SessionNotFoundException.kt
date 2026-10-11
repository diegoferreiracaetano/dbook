package com.dbook.domain.identity

class SessionNotFoundException(familyId: String) : RuntimeException("Session not found: $familyId")
