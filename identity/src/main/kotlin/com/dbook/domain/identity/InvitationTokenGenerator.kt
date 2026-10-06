package com.dbook.domain.identity

interface InvitationTokenGenerator {
    fun generate(): String
}
