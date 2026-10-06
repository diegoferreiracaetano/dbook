package com.dbook.application.notification

import com.dbook.application.common.countOutcome
import com.dbook.domain.notification.DeliveryClaim
import com.dbook.domain.notification.NotificationChannel
import com.dbook.domain.notification.NotificationDeliveryLog
import com.dbook.domain.notification.NotificationPreferenceRepository
import com.dbook.domain.notification.effectivePreferences
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.observation.annotation.Observed
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/** At least one channel failed: the message stays on the queue and comes back, and only the failed ones are retried. */
class NotificationDeliveryException(val failedChannels: List<NotificationChannel>) :
    RuntimeException("Could not deliver through ${failedChannels.joinToString()}")

/**
 * Turns an event into notifications, one channel at a time, and tolerates everything a queue does to a message:
 * - **duplicate delivery:** each (event, channel) is claimed in the delivery log; one already done is not done twice;
 * - **a channel that fails does not stop the others:** every channel is tried, then, if any failed, the whole message
 *   is left to be redelivered (the ones already done are skipped by the log, so only the failed ones run again), and
 *   after the queue's attempts it lands in the dead-letter queue;
 * - **the user's preferences:** a channel switched off for this type is skipped, not sent.
 */
@Observed(name = "dbook.usecase")
@Service
class ProcessNotificationEventUseCase(
    private val channels: List<NotificationChannelHandler>,
    private val deliveryLog: NotificationDeliveryLog,
    private val preferences: NotificationPreferenceRepository,
    private val meterRegistry: MeterRegistry,
) {
    private enum class Step { SENT, SKIPPED, DUPLICATE, FAILED }

    fun execute(event: NotificationEvent) {
        val content = NotificationMessages.contentFor(event)
        val switchedOff =
            effectivePreferences(preferences.findChosen(event.customerId))
                .filter { !it.enabled && it.type == event.type }
                .map { it.channel }
                .toSet()

        val failed =
            channels.filter {
                !deliverThrough(
                    it,
                    event,
                    content,
                    it.channel in switchedOff,
                )
            }.map { it.channel }
        if (failed.isNotEmpty()) {
            throw NotificationDeliveryException(failed)
        }
    }

    // true when the channel is done (sent, skipped or already done earlier), false when it failed
    private fun deliverThrough(
        handler: NotificationChannelHandler,
        event: NotificationEvent,
        content: NotificationContent,
        switchedOff: Boolean,
    ): Boolean {
        val channel = handler.channel
        val step =
            when {
                deliveryLog.claim(event.eventId, channel) == DeliveryClaim.ALREADY_DONE -> Step.DUPLICATE
                switchedOff -> Step.SKIPPED
                else -> attempt(handler, event, content)
            }
        when (step) {
            Step.SENT -> deliveryLog.markSent(event.eventId, channel)
            Step.SKIPPED -> deliveryLog.markSkipped(event.eventId, channel)
            Step.DUPLICATE, Step.FAILED -> Unit
        }
        meterRegistry.countOutcome("dbook.notification", step.name.lowercase(), "channel", channel.name.lowercase())
        return step != Step.FAILED
    }

    private fun attempt(
        handler: NotificationChannelHandler,
        event: NotificationEvent,
        content: NotificationContent,
    ): Step =
        try {
            if (handler.deliver(event, content) == ChannelOutcome.SENT) Step.SENT else Step.SKIPPED
        } catch (ex: ChannelDeliveryException) {
            log.warn("Notification {} failed on {}", event.eventId, handler.channel, ex)
            deliveryLog.markFailed(event.eventId, handler.channel, ex.message ?: "delivery failed")
            Step.FAILED
        }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(ProcessNotificationEventUseCase::class.java)
    }
}
