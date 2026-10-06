package com.dbook.domain.identity

/** Two invitations for the same address reached the database together and this one lost the race. */
class DuplicateOpenInvitationException(cause: Throwable) :
    RuntimeException("An open invitation for this e-mail already exists", cause)
