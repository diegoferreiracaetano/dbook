package com.dbook.application.notification

import com.dbook.domain.identity.EmailDeliveryException
import com.dbook.domain.identity.EmailSender
import com.dbook.domain.identity.UserRepository
import com.dbook.domain.notification.DeviceTokenRepository
import com.dbook.domain.notification.Notification
import com.dbook.domain.notification.NotificationChannel
import com.dbook.domain.notification.NotificationRepository
import com.dbook.domain.notification.PushDeliveryException
import com.dbook.domain.notification.PushSender
import org.springframework.stereotype.Service
import java.time.Clock

/** What happened when a channel tried to deliver: it went out, or there was nowhere to send it. */
enum class ChannelOutcome { SENT, SKIPPED }

/** The channel could not deliver (the mail server, the push service): the other channels still go out. */
class ChannelDeliveryException(
    message: String,
    cause: Throwable,
) : RuntimeException(message, cause)

/** One way of reaching the customer. A failure is a [ChannelDeliveryException]; "nowhere to send" is SKIPPED. */
interface NotificationChannelHandler {
    val channel: NotificationChannel

    fun deliver(
        event: NotificationEvent,
        content: NotificationContent,
    ): ChannelOutcome
}

/** The notification the customer finds in the app. The unique event id makes a repeated delivery drop itself. */
@Service
class InAppChannel(
    private val notifications: NotificationRepository,
    private val clock: Clock,
) : NotificationChannelHandler {
    override val channel = NotificationChannel.IN_APP

    override fun deliver(
        event: NotificationEvent,
        content: NotificationContent,
    ): ChannelOutcome {
        notifications.save(
            Notification(
                userId = event.customerId,
                type = event.type,
                title = content.title,
                body = content.body,
                data = event.data,
                eventId = event.eventId,
                readAt = null,
                createdAt = clock.instant(),
            ),
        )
        return ChannelOutcome.SENT
    }
}

/** The e-mail, to the address of the account, unless the account was anonymized (its address cannot receive mail). */
@Service
class EmailChannel(
    private val userRepository: UserRepository,
    private val emailSender: EmailSender,
) : NotificationChannelHandler {
    override val channel = NotificationChannel.EMAIL

    override fun deliver(
        event: NotificationEvent,
        content: NotificationContent,
    ): ChannelOutcome {
        val user = userRepository.findById(event.customerId)
        if (user == null || user.isAnonymized) {
            return ChannelOutcome.SKIPPED
        }
        try {
            emailSender.send(user.email, content.title, "Olá, ${user.name}.\n\n${content.body}\n\nEquipe DBook")
        } catch (ex: EmailDeliveryException) {
            throw ChannelDeliveryException("The e-mail could not be sent", ex)
        }
        return ChannelOutcome.SENT
    }
}

/** The push, to every device the customer registered. No device, nothing to send. */
@Service
class PushChannel(
    private val deviceTokens: DeviceTokenRepository,
    private val pushSender: PushSender,
) : NotificationChannelHandler {
    override val channel = NotificationChannel.PUSH

    override fun deliver(
        event: NotificationEvent,
        content: NotificationContent,
    ): ChannelOutcome {
        val devices = deviceTokens.findByUser(event.customerId)
        if (devices.isEmpty()) {
            return ChannelOutcome.SKIPPED
        }
        try {
            pushSender.send(devices, content.title, content.body, mapOf("type" to event.type.name) + event.data)
        } catch (ex: PushDeliveryException) {
            throw ChannelDeliveryException("The push could not be sent", ex)
        }
        return ChannelOutcome.SENT
    }
}
