package com.dbook.domain.notification

/** Sends a push to some devices. The real adapter (FCM) is an evolution; see docs/notificacoes.md. */
interface PushSender {
    /** @throws PushDeliveryException if it could not be sent. */
    fun send(
        devices: List<DeviceToken>,
        title: String,
        body: String,
        data: Map<String, Any?>,
    )
}

class PushDeliveryException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
