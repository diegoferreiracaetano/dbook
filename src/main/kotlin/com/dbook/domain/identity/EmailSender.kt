package com.dbook.domain.identity

interface EmailSender {
    fun send(
        to: String,
        subject: String,
        body: String,
    )
}
