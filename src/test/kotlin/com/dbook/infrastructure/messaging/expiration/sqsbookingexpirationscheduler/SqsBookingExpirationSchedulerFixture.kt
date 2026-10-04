package com.dbook.infrastructure.messaging.expiration.sqsbookingexpirationscheduler

import com.dbook.LocalStackSqs
import com.dbook.infrastructure.messaging.expiration.SqsBookingExpirationScheduler
import com.fasterxml.jackson.databind.ObjectMapper
import io.micrometer.tracing.Tracer
import io.micrometer.tracing.propagation.Propagator
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.Message
import java.net.URI

// A real (LocalStack) SQS, because the whole point of the adapter is the delay semantics.
abstract class SqsBookingExpirationSchedulerFixture {
    protected val sqsClient: SqsClient = LocalStackSqs.client

    protected fun createQueue(): String = LocalStackSqs.createQueue()

    protected fun schedulerFor(
        queueUrl: String,
        client: SqsClient = sqsClient,
    ) = SqsBookingExpirationScheduler(client, ObjectMapper(), Tracer.NOOP, Propagator.NOOP, queueUrl)

    protected fun receive(
        queueUrl: String,
        waitSeconds: Int,
    ): List<Message> = LocalStackSqs.receive(queueUrl, waitSeconds)

    protected fun clientFor(endpoint: URI): SqsClient = LocalStackSqs.clientFor(endpoint)
}
