package com.dbook.infrastructure.messaging.expiration

import com.dbook.domain.booking.BookingExpirationScheduler
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import software.amazon.awssdk.core.exception.SdkException
import software.amazon.awssdk.services.sqs.SqsClient
import java.time.Duration

// Runs after the booking's transaction committed, so a failure here must not surface as an
// error for a booking that exists: it is logged and swallowed on purpose. The window this
// leaves (crash between commit and send) is the classic dual-write problem — the
// transactional outbox (CHECKLIST 20.6) is the fix.
@Component
class SqsBookingExpirationScheduler(
    private val sqsClient: SqsClient,
    private val objectMapper: ObjectMapper,
    @Value("\${booking-expiration.queue-url}") private val queueUrl: String,
) : BookingExpirationScheduler {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun scheduleExpiration(
        bookingId: Long,
        after: Duration,
    ) {
        require(after.seconds in 0..MAX_DELAY_SECONDS) {
            "SQS delays must be between 0 and $MAX_DELAY_SECONDS seconds"
        }
        val body = objectMapper.writeValueAsString(BookingExpirationMessage(bookingId))
        try {
            sqsClient.sendMessage {
                it.queueUrl(queueUrl).messageBody(body).delaySeconds(after.seconds.toInt())
            }
        } catch (ex: SdkException) {
            log.error("Could not schedule the expiration of booking {}", bookingId, ex)
        }
    }

    private companion object {
        // the most a single SQS message can be delayed (15 minutes)
        const val MAX_DELAY_SECONDS = 900L
    }
}
