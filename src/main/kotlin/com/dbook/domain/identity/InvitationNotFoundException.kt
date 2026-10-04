package com.dbook.domain.identity

class InvitationNotFoundException(id: Long) : RuntimeException("Invitation not found: $id")
