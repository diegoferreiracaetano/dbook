package com.dbook

import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.utility.DockerImageName
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.Message
import software.amazon.awssdk.services.sqs.model.QueueAttributeName
import java.net.URI

// One LocalStack (SQS) per test JVM, shared by every test that needs a real queue — a
// container per fixture would pay the startup cost again for each one.
object LocalStackSqs {
    private val container: LocalStackContainer =
        LocalStackContainer(DockerImageName.parse("localstack/localstack:3.8.1"))
            .withServices(LocalStackContainer.Service.SQS)
            .apply { start() }

    val client: SqsClient = clientFor(container.getEndpointOverride(LocalStackContainer.Service.SQS))

    fun clientFor(endpoint: URI): SqsClient =
        SqsClient.builder()
            .endpointOverride(endpoint)
            .region(Region.of(container.region))
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(container.accessKey, container.secretKey),
                ),
            )
            .build()

    fun createQueue(attributes: Map<QueueAttributeName, String> = emptyMap()): String =
        client.createQueue {
            it.queueName("queue-${(1..999_999_999).random()}").attributes(attributes)
        }.queueUrl()

    /** @return the main queue's url and its dead-letter queue's url. */
    fun createQueueWithDeadLetterQueue(
        visibilityTimeoutSeconds: Int,
        maxReceiveCount: Int,
    ): Pair<String, String> {
        val deadLetterUrl = createQueue()
        val deadLetterArn =
            client.getQueueAttributes {
                it.queueUrl(deadLetterUrl).attributeNames(QueueAttributeName.QUEUE_ARN)
            }.attributes().getValue(QueueAttributeName.QUEUE_ARN)
        val redrivePolicy = """{"deadLetterTargetArn":"$deadLetterArn","maxReceiveCount":"$maxReceiveCount"}"""
        val queueUrl =
            createQueue(
                mapOf(
                    QueueAttributeName.VISIBILITY_TIMEOUT to visibilityTimeoutSeconds.toString(),
                    QueueAttributeName.REDRIVE_POLICY to redrivePolicy,
                ),
            )
        return queueUrl to deadLetterUrl
    }

    fun send(
        queueUrl: String,
        body: String,
    ) {
        client.sendMessage { it.queueUrl(queueUrl).messageBody(body) }
    }

    fun receive(
        queueUrl: String,
        waitSeconds: Int,
    ): List<Message> =
        client.receiveMessage {
            it.queueUrl(queueUrl).waitTimeSeconds(waitSeconds).maxNumberOfMessages(10)
        }.messages()
}
