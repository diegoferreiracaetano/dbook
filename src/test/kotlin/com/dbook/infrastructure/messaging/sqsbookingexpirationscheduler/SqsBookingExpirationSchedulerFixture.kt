package com.dbook.infrastructure.messaging.sqsbookingexpirationscheduler

import com.dbook.infrastructure.messaging.SqsBookingExpirationScheduler
import com.fasterxml.jackson.databind.ObjectMapper
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.utility.DockerImageName
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.Message
import java.net.URI

// A real (LocalStack) SQS, because the whole point of the adapter is the delay semantics.
abstract class SqsBookingExpirationSchedulerFixture {
    protected val sqsClient: SqsClient = clientFor(localstack.getEndpointOverride(LocalStackContainer.Service.SQS))

    protected fun createQueue(): String =
        sqsClient.createQueue { it.queueName("expiration-${(1..999_999_999).random()}") }.queueUrl()

    protected fun schedulerFor(
        queueUrl: String,
        client: SqsClient = sqsClient,
    ) = SqsBookingExpirationScheduler(client, ObjectMapper(), queueUrl)

    protected fun receive(
        queueUrl: String,
        waitSeconds: Int,
    ): List<Message> =
        sqsClient.receiveMessage {
            it.queueUrl(queueUrl).waitTimeSeconds(waitSeconds).maxNumberOfMessages(10)
        }.messages()

    protected fun clientFor(endpoint: URI): SqsClient =
        SqsClient.builder()
            .endpointOverride(endpoint)
            .region(Region.of(localstack.region))
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(localstack.accessKey, localstack.secretKey),
                ),
            )
            .build()

    private companion object {
        val localstack: LocalStackContainer =
            LocalStackContainer(DockerImageName.parse("localstack/localstack:3.8.1"))
                .withServices(LocalStackContainer.Service.SQS)
                .apply { start() }
    }
}
